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
import be.neutrinet.ispng.vpn.admin.UnlockKey;
import be.neutrinet.ispng.vpn.admin.UnlockKeys;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

/**
 *
 * @author wannes
 */
public class UserRegistration extends ResourceBase {

    @Get
    public Representation handleGet() {
        setCORSHeaders(getResponseEntity());
        String lastSegment = getReference().getLastSegment();

        if (lastSegment != null) {
            UUID id = UUID.fromString(lastSegment);
            if (id == null) {
                return error();
            }
            return new JacksonRepresentation(Registration.getActiveRegistrations().get(id));
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
                reg.timeInitiated = System.currentTimeMillis();
                reg.user = new User();
                reg.user.email = (String) data.get("email");
                Registration.getActiveRegistrations().put(reg.getId(), reg);
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
                return new JacksonRepresentation("OK");
            } else if (data.containsKey("password")) {
                String password = (String) data.get("password");
                reg.user.setPassword(password);
                Users.dao.createIfNotExists(reg.user);
                return new JacksonRepresentation(reg.user);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to handle flow", ex);
        }
        return error();
    }
}
