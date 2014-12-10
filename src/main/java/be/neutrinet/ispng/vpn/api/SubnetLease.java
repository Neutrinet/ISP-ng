package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.Clients;
import be.neutrinet.ispng.vpn.ip.IPSubnet;
import be.neutrinet.ispng.vpn.ip.IPSubnets;
import be.neutrinet.ispng.vpn.ip.SubnetLeases;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wannes on 12/1/14.
 */
public class SubnetLease extends ResourceBase {

    @Get
    public Representation getSubnetLeases() {
        try {
            if (getRequestAttributes().containsKey("id")) {
                return new JacksonRepresentation<>(SubnetLeases.dao.queryForId(getAttribute("id")));
            }

            HashMap<Client, be.neutrinet.ispng.vpn.ip.SubnetLease> ip6 = new HashMap<>();
            for (Client c : Clients.dao.queryForAll()) {
                for (be.neutrinet.ispng.vpn.ip.SubnetLease sl : SubnetLeases.dao.queryForEq("client_id", c.id)) {
                    ip6.put(c, sl);
                }
            }
            return new JacksonRepresentation<>(ip6);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to get assigned leases", ex);
        }

        return DEFAULT_ERROR;
    }

    @Put
    public Representation create(Map<String, Object> data) {
        int clientId = (int) data.get("client");
        assert clientId > 0;
        int version = (int) data.get("version");
        assert version == 6;

        if (!data.containsKey("prefix")) {
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        int v6prefix = Integer.parseInt(data.get("prefix").toString());

        if (v6prefix < Integer.parseInt(VPN.cfg.getProperty("vpn.ipv6.pool.minAllocPrefix", "48"))) {
            return clientError("IP_ADDRESS_LEASE_TOO_LARGE", Status.CLIENT_ERROR_NOT_ACCEPTABLE);
        }

        try {
            Client client = Clients.dao.queryForId("" + clientId);

            IPSubnet ips = IPSubnets.allocate(v6prefix, false, 6);

            if (ips == null) {
                return clientError("OUT_OF_ADDRESS_SPACE", Status.SERVER_ERROR_INTERNAL);
            } else {
                be.neutrinet.ispng.vpn.ip.SubnetLease sl = new be.neutrinet.ispng.vpn.ip.SubnetLease();
                sl.active = true;
                sl.client = client;
                sl.subnet = ips;
                SubnetLeases.dao.create(sl);

                return new JacksonRepresentation<>(ips);
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create subnet lease", ex);
        }

        return DEFAULT_ERROR;
    }

    @Post
    public Representation updateLease(be.neutrinet.ispng.vpn.ip.SubnetLease sl) {
        if (!getRequestAttributes().containsKey("id"))
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        if (sl == null) return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        try {
            be.neutrinet.ispng.vpn.ip.SubnetLease subnetLease = SubnetLeases.dao.queryForId(getAttribute("id"));
            mergeUpdate(subnetLease, sl);
            SubnetLeases.dao.update(subnetLease);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to update subnet lease", ex);
        }

        return new JacksonRepresentation<>(sl);
    }

    @Delete
    public Representation deleteLease() {
        if (!getRequestAttributes().containsKey("id"))
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        try {
            be.neutrinet.ispng.vpn.ip.SubnetLease sl = SubnetLeases.dao.queryForId(getAttribute("id"));
            SubnetLeases.dao.delete(sl);

            if (!getRequestAttributes().containsKey("shallow")) {
                IPSubnets.dao.delete(sl.subnet);
            }

            return new JacksonRepresentation<>(sl);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to delete subnet lease", ex);
        }

        return DEFAULT_ERROR;
    }
}
