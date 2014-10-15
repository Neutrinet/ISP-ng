package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.monitoring.DataPoint;
import be.neutrinet.ispng.vpn.*;
import com.j256.ormlite.misc.TransactionManager;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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

        try {
            User user = Users.authenticate(client.username, client.password);
            if (user != null) {
                TransactionManager.callInTransaction(VPN.cs, () -> {
                    IPAddress ipv4 = Manager.get().assign(user, client, 4);
                    IPAddress ipv6 = Manager.get().assign(user, client, 6);

                    if (ipv4 == null && ipv6 == null) {
                        vpn.denyClient(client.id, client.kid, "No IP address available");
                        return null;
                    }

                    Connection c = new Connection(userClient);
                    c.openvpnInstance = vpn.getInstanceId();
                    c.fill(client);

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
                        options.put("ifconfig-push", ipv4.address + " " + VPN.cfg.getProperty("openvpn.localip.4"));
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

                    //options.put("push route-gateway", "192.168.2.1");
                    if (ipv6 != null) {
                        IPAddress v6alloc = Manager.get().allocateIPv6FromSubnet(ipv6, user);
                        options.put("push tun-ipv6", "");
                        options.put("ifconfig-ipv6-push", v6alloc.address + "/64 " + VPN.cfg.getProperty("openvpn.localip.6"));
                        options.put("push route-ipv6", VPN.cfg.getProperty("openvpn.network.6") + "/" + VPN.cfg.getProperty("openvpn.netmask.6"));
                        // route assigned IPv6 subnet through client
                        options.put("iroute-ipv6", ipv6.address + "/64");

                        if (user.settings().get("routeIPv6TrafficOverVPN", true).equals(true)) {
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
            query.put("client_id", client.id);
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
            query.put("client_id", client.id);
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
        Logger.getLogger(getClass()).debug("Connection established " + client.id);
    }

    @Override
    public void addressInUse(Client client, String address, boolean primary) {
        try {
            Connection c = pendingConnections.remove(client.id);

            if (c == null) {
                // connection is being re-established
                return;
            }

            Connections.dao.create(c);

            for (IPAddress ip : c.addresses) {
                ip.connection = c;
                IPAddresses.dao.update(ip);
            }
        } catch (SQLException ex) {
            log.error("Failed to insert connection", ex);
        }
    }

    @Override
    public void bytecount(Client client, long bytesIn, long bytesOut) {
        HashMap<String, String> tags = new HashMap<>();
        tags.put("client", "" + client.id);
        tags.put("connection", "" + client.kid);
        tags.put("vpnInstance", "" + vpn.getInstanceId());

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
