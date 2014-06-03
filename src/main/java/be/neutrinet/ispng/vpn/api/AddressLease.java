/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 *
 * @author wannes
 */
public class AddressLease extends ResourceBase {

    @Get
    public Representation getAssignedLeases() {
        try {
            HashMap<User, List<IPAddress>> map = new HashMap<>();
            List<User> users = Users.dao.queryForAll();
            for (User u : users) {
                map.put(u, IPAddresses.dao.queryForEq("user_id", u));
            }
            
            return new JacksonRepresentation(map);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to get assigned leases", ex);
        }
        
        return DEFAULT_ERROR;
    }
    
    @Put
    public Representation create(Map<String, Object> data) {
        int userId = (int) data.get("user");
        assert userId > 0;
        int version = (int) data.get("version");
        assert version == 4 || version == 6;

        try {
            IPAddress addr = IPAddresses.findUnused(version);
            if (addr == null) {
                return clientError("OUT_OF_ADDRESSES", Status.SERVER_ERROR_INTERNAL);
            }
            addr.leasedAt = new Date();
            addr.user = Users.dao.queryForId("" + userId);
            addr.expiry = DateUtil.convert(LocalDate.now().plusDays(365L));
            IPAddresses.dao.update(addr);

            return new JacksonRepresentation(addr);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create address lease", ex);
        }

        return error();
    }
}
