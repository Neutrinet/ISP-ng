/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import be.neutrinet.ispng.vpn.ResourceBase;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Put;

/**
 *
 * @author wannes
 */
public class AddressLease extends ResourceBase {

    @Put
    public Representation create(Map<String, Object> data) {
        int userId = (int) data.get("user");
        assert userId > 0;
        int version = (int) data.get("version");
        assert version == 4 || version == 6;
        
        try {
            IPAddress addr = IPAddresses.findUnused(version);
            if (addr == null) return clientError("OUT_OF_ADDRESSES", Status.SERVER_ERROR_INTERNAL);
            addr.leasedAt = new Date();
            addr.userId = userId;
            addr.expiry = DateUtil.convert(LocalDate.now().plusDays(365L));
            IPAddresses.dao.update(addr);
            
            return new JacksonRepresentation(addr);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create address lease", ex);
        }
        
        return error();
    }
}