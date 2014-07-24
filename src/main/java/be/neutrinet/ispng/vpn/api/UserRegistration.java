/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.Registrations;
import be.neutrinet.ispng.vpn.admin.UnlockKey;
import be.neutrinet.ispng.vpn.admin.UnlockKeys;
import org.apache.log4j.Logger;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author wannes
 */
public class UserRegistration extends ResourceBase {
    
    public static SimpleDateFormat EUROPEAN_DATE_FORMAT = new SimpleDateFormat("dd-mm-yyyy");
    
    @Get
    public Representation handleGet() {
        setCORSHeaders(getResponseEntity());
        String lastSegment = getReference().getLastSegment();
        
        if (lastSegment != null) {
            UUID id = UUID.fromString(lastSegment);
            if (id == null) {
                return error();
            }
            
            Registration reg = Registration.getActiveRegistrations().get(id);
            if (reg == null) {
                try {
                    // try fetch from db
                    List<Registration> results = Registrations.dao.queryForEq("id", id);
                    if (results.size() == 1) {
                        reg = results.get(0);
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).error("Failed to query for existing registration", ex);
                }
            }
            return new JacksonRepresentation(reg);
        }
        
        return error();
    }
    
    @Post
    public Representation handlePost(Map<String, Object> data) {
        setCORSHeaders(getResponseEntity());
        try {
            if (data.get("id") != null) {
                return handleFlow(data);
            }
            
            String key = (String) data.get("key");
            List<UnlockKey> keys = UnlockKeys.dao.queryForEq("key", key);
            assert keys.size() <= 1;
            if (keys.isEmpty()) {
                return new JacksonRepresentation(new ClientError("INVALID_UNLOCK_KEY"));
            } else if (keys.get(0).usedAt != null) {
                return new JacksonRepresentation<>(new ClientError("INVALID_UNLOCK_KEY"));
            } else {
                Registration reg = new Registration(UUID.randomUUID());
                reg.timeInitiated = new Date();
                reg.user = new User();
                reg.user.email = (String) data.get("email");
                reg.unlockKey = keys.get(0);
                
                Registration.getActiveRegistrations().put(reg.getId(), reg);
                Registrations.dao.create(reg);
                return new JacksonRepresentation<>(reg);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate unlock key", ex);
        }
        
        return error();
    }
    
    private Representation handleFlow(Map<String, Object> data) {
        UUID id = UUID.fromString((String) data.get("id"));
        Registration reg = Registration.getActiveRegistrations().get(id);
        
        try {
            if (data.containsKey("user")) {
                // finalize registration
                reg.ipv4Id = (int) data.get("ipv4Id");
                reg.ipv6Id = (int) data.get("ipv6Id");
                reg.commit();
                Registration.getActiveRegistrations().remove(reg.getId());
                return new JacksonRepresentation("OK");
            } else if (data.containsKey("password")) {
                String password = (String) data.get("password");
                reg.user.setPassword(password);
                return new JacksonRepresentation(reg.user);
            } else if (data.containsKey("name")) {
                reg.user.name = (String) data.get("name");
                reg.user.lastName = (String) data.get("last-name");
                reg.user.birthDate = EUROPEAN_DATE_FORMAT.parse((String) data.get("birthdate"));
                reg.user.street = (String) data.get("address");
                reg.user.municipality = (String) data.get("town");
                reg.user.postalCode = Integer.parseInt((String) data.get("postal-code"));
                reg.user.birthPlace = (String) data.get("birthplace");
                reg.user.country = (String) data.get("country");
                
                if (reg.user.validate()) Users.dao.createIfNotExists(reg.user);
                Registrations.dao.update(reg);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to handle flow", ex);
        }
        return error();
    }
}
