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

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.openvpn.DefaultServiceListener;
import be.neutrinet.ispng.openvpn.ManagementInterface;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author wannes
 */
public final class Manager {

    private static Manager instance;
    private final Logger log = Logger.getLogger(getClass());
    protected HashMap<String, ManagementInterface> openvpnInstances;

    private Manager() {
        openvpnInstances = new HashMap<>();

        if (!VPN.cfg.containsKey("openvpn.instances")) {
            throw new Error("OpenVPN instance(s) are not configured");
        }

        if (VPN.cfg.get("openvpn.instances").toString().isEmpty()) {
            Logger.getLogger(getClass()).warn("No OpenVPN instance(s) configured, continuing without VPN support");
            return;
        }

        String[] instances = VPN.cfg.getProperty("openvpn.instances").split(";");
        for (String instance : instances) {
            String[] split = instance.split(":");
            ManagementInterface m = new ManagementInterface(new DefaultServiceListener(),
                    split[0], Integer.parseInt(split[1]));
            openvpnInstances.put(instance, m);
        }
    }

    public static Manager get() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public void dropConnection(Connection connection) {
        openvpnInstances.get(connection.openvpnInstance).killClient(connection.vpnClientId);
        try {
            connection.closed = new Date();
            Connections.dao.update(connection);
        } catch (Exception ex) {
            log.error("Failed to update dropped connection", ex);
        }
    }

    public IPAddress allocateIPv6FromSubnet(IPAddress v6subnet, Client client) throws SQLException {
        IPv6Network subnet = IPv6Network.fromString(v6subnet.address + "/" + v6subnet.netmask);
        // TODO
        IPv6Address first = subnet.getFirst().add(1);
        String addr = first.toString();

        List<IPAddress> addresses = IPAddresses.dao.queryForEq("address", addr);
        if (addresses.size() > 0) {
            return addresses.get(0);
        }

        IPAddress v6 = new IPAddress();
        v6.address = addr;
        v6.netmask = 128;
        v6.enabled = true;
        v6.leasedAt = new Date();
        v6.client = client;

        IPAddresses.dao.createOrUpdate(v6);
        return v6;
    }

    public void start() {
        for (ManagementInterface vpn : openvpnInstances.values())
            vpn.getWatchdog().start();
    }

    /**
     * If a critical system component fails, notify and quit safely
     *
     * @param reason
     */
    public void shutItDown(String reason) {

    }
}
