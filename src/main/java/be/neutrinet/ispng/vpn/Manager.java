/*
 * Manager.java
 * Copyright (C) Apr 5, 2014 Wannes De Smet
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.openvpn.ManagementInterface;
import be.neutrinet.ispng.openvpn.ServiceListener;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import com.j256.ormlite.misc.TransactionManager;
import org.apache.log4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author double-u
 */
public final class Manager {

    private static Manager instance;
    protected ManagementInterface vpn;
    protected HashMap<Integer, Connection> pendingConnections;
    private final Logger log = Logger.getLogger(getClass());

    private Manager() {
        pendingConnections = new HashMap<>();
        vpn = new ManagementInterface(new ServiceListener() {

            @Override
            public void clientConnect(Client client) {
                try {
                    Client.dao.create(client);
                    User user = Users.authenticate(client.username, client.password);
                    if (user != null) {
                        TransactionManager.callInTransaction(VPN.cs, () -> {
                            IPAddress ipv4 = assign(user, client, 4);
                            IPAddress ipv6 = assign(user, client, 6);

                            if (ipv4 == null && ipv6 == null) {
                                vpn.denyClient(client.id, client.kid, "No IP address available");
                                return null;
                            }

                            Connection c = new Connection(client.id, user);
                            if (ipv4 != null) {
                                c.addresses.add(ipv4);
                            }
                            if (ipv6 != null) {
                                c.addresses.add(ipv6);
                            }

                            pendingConnections.put(client.id, c);
                            log.info(String.format("Authorized %s (%s,%s)", client.username, client.id, client.kid));

                            LinkedHashMap<String, String> options = new LinkedHashMap<>();
                            options.put("push-reset", null);
                            if (ipv4 != null) {
                                options.put("ifconfig-push", ipv4.address + " " + VPN.cfg.getProperty("openvpn.subnet"));
                                // route the OpenVPN server over the default gateway, not over the VPN itself
                                InetAddress addr = Inet4Address.getByName(VPN.cfg.getProperty("openvpn.publicaddress"));
                                options.put("push route", addr.getHostAddress() + " 255.255.255.255 net_gateway");
                            }

                            //options.put("push route-gateway", "192.168.2.1");
                            if (ipv6 != null) {
                                options.put("ifconfig-ipv6-push", ipv6.address + "/ fdef:abcd:ef::1");
                            }
                            //options.put("push route-ipv6", "fdef:2f5:d792:de63::/64");

                            vpn.authorizeClient(client.id, client.kid, options);
                            return null;
                        });

                    } else {
                        log.info(String.format("Refused %s (%s,%s)", client.username, client.id, client.kid));
                        vpn.denyClient(client.id, client.kid, "Invalid user/password combination");
                    }
                } catch (SQLException ex) {
                    log.error("Failed to insert client", ex);
                }
            }

            @Override
            public void clientDisconnect(Client client) {
                try {
                    List<Connection> cons = Connections.dao.queryForEq("clientId", client.id);
                    if (cons.isEmpty()) {
                        return;
                    }

                    Connection c = cons.get(0);
                    c.closed = new Date();
                    c.active = false;

                    Connections.dao.update(c);

                    for (IPAddress ip : c.addresses) {
                        ip.connection = null;
                        IPAddresses.dao.update(ip);
                    }
                } catch (SQLException ex) {
                    log.error("Failed to insert client", ex);
                }
            }

            @Override
            public void connectionEstablished(Client client) {
                Logger.getLogger(getClass()).debug("Connection established " + client.id);
            }

            @Override
            public void addressInUse(Client client, String address, boolean primary) {
                try {
                    Connection c = pendingConnections.remove(client.id);
                    Connections.dao.create(c);
                    for (IPAddress ip : c.addresses) {
                        ip.connection = c;
                        IPAddresses.dao.update(ip);
                    }
                } catch (SQLException ex) {
                    log.error("Failed to insert connection", ex);
                }
            }
        });

    }

    protected IPAddress assign(User user, Client client, int version) throws SQLException {
        List<IPAddress> addrs = IPAddresses.forUser(user, version);
        if (addrs.isEmpty()) {
            IPAddress unused = IPAddresses.findUnused(version);

            if (unused == null) {
                log.info(String.format("Could not allocate IPv%s address for user %s (%s,%s)", version, client.username, client.id, client.kid));
                return null;
            }

            unused.user = user;
            unused.leasedAt = new Date();
            unused.expiry = DateUtil.convert(LocalDate.now().plusDays(1L));
            IPAddresses.dao.update(unused);
            addrs.add(unused);

            if (version == 6) {
                // We've assigned a subnet, now assign an address out of the subnet
                IPv6Network subnet = IPv6Network.fromString(unused.address + "/" + unused.netmask);
                // TODO
                IPv6Address first = subnet.getFirst();
                IPAddress v6 = new IPAddress();
                v6.address = first.toString().substring(0, first.toString().length() - 3);
                v6.netmask = 128;
                v6.enabled = true;
                v6.leasedAt = new Date();
                v6.user = user;

                IPAddresses.dao.createOrUpdate(v6);
            }
        }

        return addrs.get(0);
    }

    public void start() {
        vpn.recover();
    }

    /**
     * If a critical system component fails, notify and quit safely
     *
     * @param reason
     */
    public void shutItDown(String reason) {

    }

    public static Manager get() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }
}
