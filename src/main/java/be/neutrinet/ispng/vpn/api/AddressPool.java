/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import org.apache.log4j.Logger;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author wannes
 */
public class AddressPool extends ResourceBase {

    @Put
    public Representation addAddressesToPool(Map<String, Object> data) {
        int version = (int) data.get("version");
        String subnet = (String) data.get("subnet");
        String purpose = data.get("purpose").toString();
        if (purpose == null) purpose = IPAddress.Purpose.CLIENT_ASSIGN;
        int v6prefix = 64;
        if (data.containsKey("v6prefix")) v6prefix = (int) data.get("v6prefix");

        try {
            switch (version) {
                case 4:
                    IPAddresses.addv4SubnetToPool(subnet, purpose);
                    return DEFAULT_SUCCESS;
                case 6:
                    IPAddresses.addv6SubnetToPool(subnet, purpose, v6prefix);
                    return DEFAULT_SUCCESS;
                default:
                    return new JacksonRepresentation(new ClientError("ILLEGAL_INPUT"));
            }
        } catch (NumberFormatException ex) {
            return new JacksonRepresentation(new ClientError("ILLEGAL_INPUT", ex));
        }
    }

    @Get
    public Representation getAddressPool() {
        try {
            return new JacksonRepresentation(IPAddresses.dao.queryForAll());
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to get address pool details", ex);
        }

        return error();
    }
}
