package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.Policy;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.util.UUID;

/**
 * Created by wannes on 3/21/15.
 */
public class UserPermissions extends ResourceBase {

    @Get
    public Representation getPermissions() {
        if (!getRequestAttributes().containsKey("user"))
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        String id = getAttribute("user");

        User user = Users.queryForId(id);
        if (!Policy.get().canAccess(getLoggedInUser(), user))
            return clientError("UNAUTHORIZED", Status.CLIENT_ERROR_FORBIDDEN);

        Permissions permissions = new Permissions();
        permissions.build(user.id);
        return new JacksonRepresentation<>(permissions);
    }

    protected class Permissions {
        public boolean isAdmin;
        public boolean isService;

        public void build(UUID user) {
            this.isAdmin = Users.isAdmin(user);
            this.isService = Users.isRelatedService(user);
        }
    }
}
