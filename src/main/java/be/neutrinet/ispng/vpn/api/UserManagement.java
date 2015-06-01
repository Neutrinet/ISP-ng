/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.Policy;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author wannes
 */
public class UserManagement extends ResourceBase {

    @Get
    public Representation get() {
        if (!sessionAvailable()) return DEFAULT_ERROR;

        try {
            if (!getRequestAttributes().containsKey("user") ||
                    getAttribute("user").equals("all")) {
                List<User> users = Users.queryForAll();
                return new JacksonRepresentation(Policy.filterAccessible(getSessionToken().get().getUser(), users));
            }

            String id = getAttribute("user");

                User user;

            List<User> users = Users.query("globalId", UUID.fromString(id));
                if (users.isEmpty()) {
                    return clientError("NO_SUCH_OBJECT", Status.SUCCESS_NO_CONTENT);
                } else {
                    user = users.get(0);
                }

                if (Policy.get().canAccess(getSessionToken().get().getUser(), user)) {
                    return new JacksonRepresentation(user);
                } else {
                    return clientError("FORBIDDEN", Status.CLIENT_ERROR_BAD_REQUEST);
                }

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve users", ex);
        }

        return DEFAULT_ERROR;
    }
    
    @Post
    public Representation update(User user) {
        if (!sessionAvailable()) return DEFAULT_ERROR;

        if (!getRequestAttributes().containsKey("user") ||
                getAttribute("user").equals("all")) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        UUID userId = UUID.fromString(getAttribute("user"));
        if (!userId.equals(user.globalId)) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            User old = Users.queryForId("" + userId);
            if (!Policy.get().canModify(getSessionToken().get().getUser(), old)) {
                return clientError("FORBIDDEN", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            if (getQueryValue("changePassword") != null) {
                // Deserialized object contains plaintext new password
                // the setPassword is invoked by Restlet, set the hash directly (do not rehash)
                old.setRawPassword(user.getPassword());
                Users.update(old);
                return new JacksonRepresentation<>(old);
            }

            ArrayList<String> prohibitedFields = new ArrayList<>();
            prohibitedFields.add("password");
            Optional<User> optional = mergeUpdate(old, user, prohibitedFields);

            if (optional.isPresent()) {
                Users.update(optional.get());
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to update user", ex);
        }

        return DEFAULT_ERROR;
    }
}
