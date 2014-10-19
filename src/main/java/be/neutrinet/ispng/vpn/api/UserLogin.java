package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import java.util.Map;

/**
 * Created by wannes on 7/27/14.
 */
public class UserLogin extends ResourceBase {

    @Post
    public Representation login(Map<String, String> data) {
        if (!data.containsKey("user") || !data.containsKey("password")) {
            return error();
        }

        User user = Users.authenticate(data.get("user"), data.get("password"));
        return new JacksonRepresentation(user);
    }
}
