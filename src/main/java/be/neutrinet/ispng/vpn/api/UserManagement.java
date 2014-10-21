/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.Optional;

/**
 *
 * @author wannes
 */
public class UserManagement extends ResourceBase {

    @Get
    public Representation get() {
        try {
            if (!getRequestAttributes().containsKey("user") ||
                    getAttribute("user").equals("all")) {
                return new JacksonRepresentation(Users.dao.queryForAll());
            }

            int userId = Integer.parseInt(getAttribute("user").toString());

            if (!Users.dao.idExists("" + userId)) {
                return new JacksonRepresentation(new ClientError("NO_SUCH_OBJECT"));
            }

            return new JacksonRepresentation(Users.dao.queryForId("" + userId));
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve users", ex);
        }

        return DEFAULT_ERROR;
    }
    
    @Post
    public Representation update(User user) {
        if (!getRequestAttributes().containsKey("user") ||
                getAttribute("user").equals("all")) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        int userId = Integer.parseInt(getAttribute("user"));
        if (userId != user.id) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            User old = Users.dao.queryForId("" + userId);
            Optional<User> optional = mergeUpdate(old, user);

            if (optional.isPresent()) {
                Users.dao.update(optional.get());
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to update user", ex);
        }

        return DEFAULT_ERROR;
    }
}
