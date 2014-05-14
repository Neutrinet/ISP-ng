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

import be.neutrinet.ispng.Connections;
import be.neutrinet.ispng.IPAddresses;
import be.neutrinet.ispng.Users;
import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.openvpn.ManagementInterface;
import be.neutrinet.ispng.openvpn.ServiceListener;
import com.j256.ormlite.misc.TransactionManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
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
                            Connection c = new Connection(client.id, user.id, ipv4.id);
                            IPAddress ipv6 = assign(user, client, 6);

                            pendingConnections.put(client.id, c);
                            log.info(String.format("Authorized %s (%s,%s)", client.username, client.id, client.kid));

                            LinkedHashMap<String, String> options = new LinkedHashMap<>();
                            options.put("push-reset", null);
                            options.put("ifconfig-push", ipv4.address + " 255.255.255.0");
                            //options.put("push route", "192.168.2.0 255.255.255.0");
                            //options.put("push route-gateway", "192.168.2.1");
                            options.put("ifconfig-ipv6-push", ipv6.address + "/64 fdef:2f5:d792:de63::1");
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

                } catch (SQLException ex) {
                    log.error("Failed to insert client", ex);
                }
            }

            @Override
            public void connectionEstablished(Client client) {
                try {
                    Connection c = pendingConnections.remove(client.id);
                    Connections.dao.create(c);
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
                vpn.denyClient(client.id, client.kid, "No IPv" + version + " address available");
                return null;
            }

            unused.userId = user.id;
            IPAddresses.dao.update(unused);
            addrs.add(unused);
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
