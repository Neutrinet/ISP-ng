package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.SessionManager;
import be.neutrinet.ispng.security.SessionToken;
import be.neutrinet.ispng.security.SessionTokens;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * Created by wannes on 7/27/14.
 */
public class UserLogin extends ResourceBase {

    @Get
    public Representation get() {
        if (getRequest().getCookies().getFirstValue("Session") != null) {
            try {
                UUID token = UUID.fromString(getRequest().getCookies().getFirstValue("Session"));
                return new JacksonRepresentation(SessionTokens.dao.queryForEq("token", token).get(0));
            } catch (SQLException ex) {
                Logger.getLogger(getClass()).error("Failed to get current session", ex);
            }
        }

        return DEFAULT_SUCCESS;
    }

    @Post
    public Representation login(Map<String, String> data) {
        if (!data.containsKey("user") || !data.containsKey("password")) {
            return error();
        }

        User user = Users.authenticate(data.get("user"), data.get("password"));
        if (user != null) {
            SessionToken token = SessionManager.createSessionToken(user, getRequest().getClientInfo().getAddress());
            getResponse().getCookieSettings().add("Session", token.getToken().toString());
            return new JacksonRepresentation(token);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Authentication failed");
            return DEFAULT_ERROR;
        }
    }
}
