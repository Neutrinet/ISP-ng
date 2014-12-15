package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.monitoring.DataPoint;
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

    public DefaultServiceListener() {
        pendingConnections = new HashMap<>();

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

        if (!userClient.enabled) vpn.denyClient(client.id, client.kid, "Client is disabled");

        try {
            User user = Users.authenticate(client.username, client.password);
            if (user != null) {
                TransactionManager.callInTransaction(VPN.cs, () -> {
                    Optional<IPAddress> ipv4 = userClient.leases.stream().filter(addr -> addr.ipVersion == 4).findFirst();

                    if (!ipv4.isPresent() && userClient.subnetLeases.isEmpty()) {
                        vpn.denyClient(client.id, client.kid, "No IP address or subnet leases assigned");
                        return null;
                    }

                    Connection c = new Connection(userClient);
                    c.openvpnInstance = vpn.getInstanceId();
                    c.fill(client);

                    if (ipv4.isPresent()) {
                        c.addresses.add(ipv4.get());
                    }

                    pendingConnections.put(client.id, c);
                    log.info(String.format("Authorized %s (%s,%s)", client.username, client.id, client.kid));

                    LinkedListMultimap<String, String> options = LinkedListMultimap.create();
                    options.put("push-reset", null);

                    if (ipv4.isPresent()) {
                        options.put("ifconfig-push", ipv4.get().address + " " + VPN.cfg.getProperty("openvpn.localip.4"));
                        options.put("push route", VPN.cfg.getProperty("openvpn.network.4") + " " +
                                VPN.cfg.getProperty("openvpn.netmask.4") + " " + VPN.cfg.getProperty("openvpn.localip.4"));
                        // route the OpenVPN server over the default gateway, not over the VPN itself
                        InetAddress[] addr = InetAddress.getAllByName(VPN.cfg.getProperty("openvpn.publicaddress"));
                        for (InetAddress address : addr) {
                            if (address.getAddress().length == 4) {
                                options.put("push route", address.getHostAddress() + " 255.255.255.255 net_gateway");
                            }
                        }

                        if (user.settings().get("routeIPv4TrafficOverVPN", true).equals(true)) {
                            options.put("push redirect-gateway", "def1");
                        }
                    }

                    if (!userClient.subnetLeases.isEmpty()) {
                        options.put("push tun-ipv6", "");

                        for (SubnetLease lease : userClient.subnetLeases) {
                            String firstAddress = lease.subnet.subnet.substring(0, lease.subnet.subnet.indexOf('/')) + "1";
                            // Why /64? See https://community.openvpn.net/openvpn/ticket/264
                            options.put("ifconfig-ipv6-push", firstAddress + "/64" + " " + VPN.cfg.getProperty("vpn.ipv6.localip"));
                            options.put("push route-ipv6", VPN.cfg.getProperty("vpn.ipv6.network") + "/" + VPN.cfg.getProperty("vpn.ipv6.prefix")
                                    + " " + VPN.cfg.getProperty("vpn.ipv6.localip"));
                            // route assigned IPv6 subnet through client
                            options.put("iroute-ipv6", lease.subnet.subnet);
                        }

                        if (user.settings().get("ip.route.ipv6.defaultRoute").isPresent()) {
                            //options.put("push redirect-gateway-ipv6", "def1");
                            options.put("push route-ipv6", "2000::/3");
                        }
                    }

                    if (VPN.cfg.containsKey("openvpn.ping")) {
                        options.put("push ping", VPN.cfg.get("openvpn.ping").toString());
                        if (VPN.cfg.containsKey("openvpn.pingRestart")) {
                            options.put("push ping-restart", VPN.cfg.get("openvpn.pingRestart").toString());
                        }
                    } else {
                        log.warn("No ping and set, will cause spurious connection resets");
                    }

                    vpn.authorizeClient(client.id, client.kid, options);
                    return null;
                });

            } else {
                log.info(String.format("Refused %s (%s,%s)", client.username, client.id, client.kid));
                vpn.denyClient(client.id, client.kid, "Invalid user/password combination");
            }
        } catch (SQLException ex) {
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
        HashMap<String, String> tags = new HashMap<>();
        tags.put("client", "" + client.id);
        tags.put("connection", "" + client.kid);
        tags.put("vpnInstance", "" + vpn.getInstanceId().replace(':', '-'));

        DataPoint bytesInDataPoint = new DataPoint();
        bytesInDataPoint.metric = "vpn.client.bytesIn";
        bytesInDataPoint.timestamp = System.currentTimeMillis();
        bytesInDataPoint.value = bytesIn;
        bytesInDataPoint.tags = tags;

        DataPoint bytesOutDataPoint = new DataPoint();
        bytesOutDataPoint.metric = "vpn.client.bytesOut";
        bytesOutDataPoint.timestamp = System.currentTimeMillis();
        bytesOutDataPoint.value = bytesOut;
        bytesOutDataPoint.tags = tags;

        VPN.monitoringAgent.addDataPoint(bytesInDataPoint);
        VPN.monitoringAgent.addDataPoint(bytesOutDataPoint);
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
