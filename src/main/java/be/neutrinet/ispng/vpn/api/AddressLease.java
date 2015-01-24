/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.Clients;
import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * @author wannes
 */
public class AddressLease extends ResourceBase {

    @Get
    public Representation getAssignedLeases() {
        try {
            if (getRequestAttributes().containsKey("id")) {
                return new JacksonRepresentation<>(IPAddresses.dao.queryForId(getAttribute("id")));
            }

            HashMap<Client, List<IPAddress>> map = new HashMap<>();
            List<Client> clients = Clients.dao.queryForAll();

            for (Client c : clients) {
                map.put(c, IPAddresses.dao.queryForEq("client_id", c.id));
            }

            return new JacksonRepresentation(map);
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
        assert version == 4 || version == 6;
        String purpose = data.get("purpose").toString();
        if (purpose == null) purpose = IPAddress.Purpose.CLIENT_ASSIGN;

        try {
            Client client = Clients.dao.queryForId("" + clientId);
            List<IPAddress> addresses = IPAddresses.forUser(client.user, version);
            if (addresses.size() > 1)
                return clientError("MAX_IP_ADDRESSES_EXCEEDED", Status.CLIENT_ERROR_NOT_ACCEPTABLE);

            Optional<IPAddress> oaddr = IPAddresses.findUnused(version);

            if (!oaddr.isPresent())
                return clientError("OUT_OF_ADDRESS_SPACE", Status.SERVER_ERROR_INTERNAL);
            else {
                IPAddress addr = oaddr.get();
                addr.leasedAt = new Date();
                addr.client = Clients.dao.queryForId("" + clientId);
                addr.expiry = DateUtil.convert(LocalDate.now().plusDays(365L));
                addr.purpose = purpose;
                IPAddresses.dao.update(addr);
                return new JacksonRepresentation(addr);
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create address lease", ex);
        }

        return error();
    }

    @Post
    public Representation updateLease(IPAddress addr) {
        if (!getRequestAttributes().containsKey("id"))
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        if (addr == null) return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        try {
            IPAddress ip = IPAddresses.dao.queryForId(getAttribute("id"));
            mergeUpdate(ip, addr);
            IPAddresses.dao.update(ip);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to update subnet lease", ex);
        }

        return new JacksonRepresentation<>(addr);
    }


    @Delete
    public Representation deleteLease(Map<String, String> data) {
        int ipVersion = Integer.parseInt(data.get("version"));

        try {
            if (data.containsKey("user")) {
                List<IPAddress> addrs = IPAddresses.forClient(Clients.dao.queryForId(data.get("client")), ipVersion);
                for (IPAddress addr : addrs) {
                    addr.client = Clients.NONE;
                    IPAddresses.dao.update(addr);
                }
            } else {
                if (!getRequestAttributes().containsKey("id"))
                    return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
                IPAddress ipAddress = IPAddresses.dao.queryForId(getAttribute("id"));
                ipAddress.client = null;
                ipAddress.connection = null;
                IPAddresses.dao.update(ipAddress);
            }
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to modify address lease", ex);
        }

        return error();
    }
}
