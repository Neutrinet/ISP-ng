package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.vpn.*;
import be.neutrinet.ispng.vpn.ip.SubnetLease;
import com.google.common.collect.LinkedListMultimap;
import com.j256.ormlite.misc.TransactionManager;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Created by wannes on 14/10/14.
 */
public class DefaultServiceListener implements ServiceListener {

    public final static String YES = "yes";
    private final Logger log = Logger.getLogger(getClass());
    protected ManagementInterface vpn;
    protected boolean acceptNewConnections, acceptConnections;
    protected HashMap<Integer, Connection> pendingConnections;
    protected Monitoring monitoringAgent;

    public DefaultServiceListener() {
        pendingConnections = new HashMap<>();
        monitoringAgent = new Monitoring();

        Config.get().getAndWatch("OpenVPN/connections/accept", YES, value -> acceptConnections = YES.equals(value));
        Config.get().getAndWatch("OpenVPN/connections/acceptNew", YES, value -> acceptNewConnections = YES.equals(value));
    }

    @Override
    public void clientConnect(Client client) {
        if (!acceptConnections || !acceptNewConnections) {
            vpn.denyClient(client.id, client.kid, "Connection denied");
            return;
        }

        be.neutrinet.ispng.vpn.Client userClient = be.neutrinet.ispng.vpn.Client.match(client).orElseGet(() -> be.neutrinet.ispng.vpn.Client.create(client));

        if (!userClient.enabled) {
            vpn.denyClient(client.id, client.kid, "Client is disabled");
            return;
        }

        try {
            User user = Users.authenticate(client.username, client.password);
            if (user != null) {
                TransactionManager.callInTransaction(VPN.cs, () -> {
                    log.debug(String.format("[%s] starting authentication", client.username));
                    Optional<IPAddress> ipv4;
                    if (userClient.leases == null) {
                        ipv4 = Optional.empty();
                        log.debug(String.format("[%s] no ipv4", client.username));
                    }
                    else {
                        ipv4 = userClient.leases.stream().filter(addr -> addr.ipVersion == 4).findFirst();
                        log.debug(String.format("[%s] ipv4: %s", client.username, ipv4.get().address));
                    }

                    /*if (!ipv4.isPresent() && userClient.subnetLeases.isEmpty()) {
                        vpn.denyClient(client.id, client.kid, "No IP address or subnet leases assigned");
                        return null;
                    }*/

                    Connection c = new Connection(userClient);
                    c.openvpnInstance = vpn.getInstanceId();
                    c.fill(client);

                    if (ipv4.isPresent()) {
                        c.addresses.add(ipv4.get());
                        log.debug(String.format("[%s] adding ipv4 to client", client.username));
                    }

                    pendingConnections.put(client.id, c);
                    log.info(String.format("Authorized %s (%s,%s)", client.username, client.id, client.kid));
                    long startHandling = System.currentTimeMillis();

                    LinkedListMultimap<String, String> options = LinkedListMultimap.create();
                    options.put("push-reset", null);

                    if (ipv4.isPresent()) {
                        options.put("ifconfig-push", ipv4.get().address + " " + VPN.cfg.getProperty("openvpn.netmask.4"));

                        log.debug(String.format("[%s] pushing 'ifconfig-push' to the client: %s", client.username, ipv4.get().address + " " +
                                    VPN.cfg.getProperty("openvpn.netmask.4")));

                        options.put("push route", VPN.cfg.getProperty("openvpn.network.4") + " " +
                                VPN.cfg.getProperty("openvpn.netmask.4") + " " + VPN.cfg.getProperty("openvpn.localip.4"));

                        log.debug(String.format("[%s] push route: %s", client.username,
                                    VPN.cfg.getProperty("openvpn.network.4") + " " + VPN.cfg.getProperty("openvpn.netmask.4") +
                                    " " + VPN.cfg.getProperty("openvpn.localip.4")));

                        // route the OpenVPN server over the default gateway, not over the VPN itself
                        InetAddress[] addr = InetAddress.getAllByName(VPN.cfg.getProperty("openvpn.publicaddress"));
                        for (InetAddress address : addr) {
                            if (address.getAddress().length == 4) {
                                options.put("push route", address.getHostAddress() + " 255.255.255.255 net_gateway");

                                log.debug(String.format("[%s] push route: %s", client.username,
                                            address.getHostAddress() + " 255.255.255.255 net_gateway"));

                            }
                        }

                        if (user.settings().get("routeIPv4TrafficOverVPN", true).equals(true)) {
                            options.put("push redirect-gateway", "def1");
                            log.debug(String.format("[%s] push redirect-gateway: def1", client.username));

                            options.put("push route-gateway", VPN.cfg.getProperty("openvpn.localip.4"));
                            log.debug(String.format("[%s] push route-gateway: %s", client.username,
                                        VPN.cfg.getProperty("openvpn.localip.4")));
                        }
                    }

                    options.put("push tun-ipv6", "");
                    log.debug(String.format("[%s] push tun-ipv6: <empty_string>", client.username));

                    IPAddress interconnect = userClient.getOrCreateInterconnectIP(6);
                    // Why /64? See https://community.openvpn.net/openvpn/ticket/264
                    options.put("ifconfig-ipv6-push", interconnect.address + "/64" + " " + VPN.cfg.getProperty("vpn.ipv6.interconnect"));
                    log.debug(String.format("[%s] push ifconfig-ipv6-push: %s", client.username, interconnect.address + "/64" + " " + VPN.cfg.getProperty("vpn.ipv6.interconnect")));

                    if (!ipv4.isPresent()) {
                        /* because OpenVPN does not acknowledge that IPv6-only connectivity is a thing now, we need
                        to assign an ephemeral IPv4 address.
                         */

                    }

                    if (user.settings().get("ip.route.ipv6.defaultRoute", true).equals(true)) {
                        //options.put("push redirect-gateway-ipv6", "def1");
                        options.put("push route-ipv6", "2000::/3");

                        log.debug(String.format("[%s] push route-ipv6: 2000::/3", client.username));
                    } else {
                        log.debug(String.format("[%s] DON'T push route-ipv6: 2000::/3", client.username));
                    }

                    if (!userClient.subnetLeases.isEmpty()) {
                        for (SubnetLease lease : userClient.subnetLeases) {
                            options.put("push route-ipv6", VPN.cfg.getProperty("vpn.ipv6.network") + "/" + VPN.cfg.getProperty("vpn.ipv6.prefix")
                                    + " " + VPN.cfg.getProperty("vpn.ipv6.localip"));

                            log.debug(String.format("[%s] push route-ipv6: %s", client.username,
                                        VPN.cfg.getProperty("vpn.ipv6.network") + "/" + VPN.cfg.getProperty("vpn.ipv6.prefix")
                                        + " " + VPN.cfg.getProperty("vpn.ipv6.localip")));

                            // route assigned IPv6 subnet through client
                            options.put("iroute-ipv6", lease.subnet.subnet);
                            log.debug(String.format("[%s] push route-ipv6: %s", client.username, lease.subnet.subnet));

                            options.put("setenv-safe DELEGATED_IPv6_PREFIX", lease.subnet.subnet);
                            log.debug(String.format("[%s] setenv-safe DELEGATED_IPv6_PREFIX: %s", client.username, lease.subnet.subnet));
                        }
                    } else {
                        log.debug(String.format("[%s] no SubnetLease, don't push ipv6 route, iroute and DELEGATED_IPv6_PREFIX",
                                    client.username));
                    }

                    if (VPN.cfg.containsKey("openvpn.ping")) {
                        options.put("push ping", VPN.cfg.get("openvpn.ping").toString());
                        log.debug(String.format("[%s] push ping: %s", client.username, VPN.cfg.get("openvpn.ping").toString()));

                        if (VPN.cfg.containsKey("openvpn.pingRestart")) {
                            options.put("push ping-restart", VPN.cfg.get("openvpn.pingRestart").toString());
                            log.debug(String.format("[%s] push ping-restart: %s", client.username, VPN.cfg.get("openvpn.pingRestart").toString()));
                        }
                    } else {
                        log.warn(String.format("[%s] No ping and set, will cause spurious connection resets", client.username));
                    }

                    vpn.authorizeClient(client.id, client.kid, options);
                    log.info(String.format("Client %s (%s,%s) handled in " +
                                    (System.currentTimeMillis() - startHandling) / 1000 + " seconds",
                            client.username, client.id, client.kid));

                    return null;
                });

            } else {
                log.info(String.format("Refused %s (%s,%s), invalid user/password combination", client.username, client.id, client.kid));
                vpn.denyClient(client.id, client.kid, "Invalid user/password combination");
            }
        } catch (Exception ex) {
            log.error("Failed to set client configuration", ex);
        }
    }

    @Override
    public void clientDisconnect(Client client) {
        try {
            HashMap<String, Object> query = new HashMap<>();
            query.put("vpnClientId", client.id);
            query.put("openvpnInstance", vpn.getInstanceId());
            List<Connection> cons = Connections.dao.queryForFieldValues(query);
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
            log.error("Failed to update client", ex);
        }
    }

    @Override
    public void clientReAuth(Client client) {
        if (!acceptConnections) {
            vpn.denyClient(client.id, client.kid, "Reconnection denied");
        }

        try {
            // FIX THIS
            HashMap<String, Object> query = new HashMap<>();
            be.neutrinet.ispng.vpn.Client vc = be.neutrinet.ispng.vpn.Client.match(client).get();
            query.put("client_id", vc.id);
            query.put("openvpnInstance", vpn.getInstanceId());
            List<Connection> connections = Connections.dao.queryForFieldValues(query);

            if (connections.stream().filter(c -> c.active).count() > 0) {
                vpn.authorizeClient(client.id, client.kid);
                return;
            }

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to reauth connection " + client.id);
        }

        vpn.denyClient(client.id, client.kid, "Reconnection failed");
    }

    @Override
    public void connectionEstablished(Client client) {
        Connection c = pendingConnections.remove(client.id);

        if (c == null) {
            log.debug("Re-established connection" + client.id);
            return;
        } else {
            log.debug("Connection established " + client.id);
        }

        try {
            Connections.dao.create(c);
        } catch (SQLException ex) {
            log.error("Failed to insert connection", ex);
        }
    }

    @Override
    public void addressInUse(Client client, String address, boolean primary) {
        try {
            List<IPAddress> addresses = IPAddresses.dao.queryForEq("address", address);

            if (addresses.isEmpty()) {
                return;
            }

            if (addresses.size() > 1) {
                throw new Error("Address " + address + " has multiple instances in the address pool");
            }

            IPAddress addr = addresses.get(0);
            addr.connection = pendingConnections.get(client.id);
            IPAddresses.dao.update(addr);
        } catch (SQLException ex) {
            log.error("Failed to update IP addresses", ex);
        }
    }

    @Override
    public void bytecount(Client client, long bytesIn, long bytesOut) {
        monitoringAgent.byteCount(client, bytesIn, bytesOut, vpn.getInstanceId().replace(':', '-'));
    }

    @Override
    public void setManagementInterface(ManagementInterface mgmt) {
        this.vpn = mgmt;
    }

    @Override
    public void managementConnectionEstablished() {
        // Check if management interface has been set
        assert vpn != null;

        Config.get().getAndWatch("OpenVPN/monitoring/bandwidth", YES, value -> {
            if (YES.equals(value)) {
                vpn.setBandwidthMonitoringInterval(1);
            } else if (!YES.equals(value)) {
                vpn.setBandwidthMonitoringInterval(0);
            }
        });
    }
}
