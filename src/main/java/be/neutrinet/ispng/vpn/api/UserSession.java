package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.SessionToken;
import be.neutrinet.ispng.security.SessionTokens;
import be.neutrinet.ispng.vpn.Users;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by wannes on 2/22/15.
 */
public class UserSession extends ResourceBase {
    @Get
    public Representation getSessionDetails() {
        if (!Users.isRelatedService(getLoggedInUser()))
            return clientError("ACCESS_DENIED", Status.CLIENT_ERROR_UNAUTHORIZED);
        if (getAttribute("session") == null) return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        String session = getAttribute("session");
        try {
            HashMap<String, Object> sessionDetails = new HashMap<>();
            SessionToken st = SessionTokens.dao.queryForEq("token", UUID.fromString(session)).get(0);
            sessionDetails.put("details", st);
            sessionDetails.put("isAdmin", Users.isAdmin(st.getUser()));
            return new JacksonRepresentation<>(sessionDetails);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve session", ex);
        }

        return DEFAULT_ERROR;
    }
}
