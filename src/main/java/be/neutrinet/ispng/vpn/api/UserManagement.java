/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.Users;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

/**
 *
 * @author wannes
 */
public class UserManagement extends ResourceBase {

    @Get
    public Representation get() {
        try {
            if (!getRequest().getAttributes().containsKey("user")) {
                return new JacksonRepresentation(Users.dao.queryForAll());
            }

            int userId = Integer.parseInt(getRequestAttributes().get("user").toString());

            if (!Users.dao.idExists("" + userId)) {
                return new JacksonRepresentation(new ClientError("NO_SUCH_OBJECT"));
            }

            return new JacksonRepresentation(Users.dao.queryForId("" + userId));
        } catch (Exception ex) {
            return DEFAULT_ERROR;
        }
    }
    
    @Post
    public Representation update() {
        return DEFAULT_ERROR;
    }
}
