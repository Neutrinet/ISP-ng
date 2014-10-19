package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import net.wgr.core.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by wannes on 8/10/14.
 */
public class UserSettings extends ResourceBase {

    private User getUserFromRequest() {
        String userId = getRequestAttributes().get("user").toString();

        try {
            User user = null;
            if (!StringUtils.isNumeric(userId))
                user = Users.dao.queryForEq("email", userId).get(0);
            else
                user = Users.dao.queryForId(userId);

            return user;
        } catch (SQLException e) {
            Logger.getLogger(getClass()).error("Unknown user", e);
        }

        return null;
    }

    @Get
    public JacksonRepresentation get() {
        User user = getUserFromRequest();
        if (user == null) return clientError("NO_SUCH_USER", Status.CLIENT_ERROR_BAD_REQUEST);
        return new JacksonRepresentation(user.settings().get(getRequestAttributes().get("setting").toString()));
    }

    @Post
    public JacksonRepresentation post(Map<String, Object> value) {
        User user = getUserFromRequest();
        if (user == null) return clientError("NO_SUCH_USER", Status.CLIENT_ERROR_BAD_REQUEST);
        user.settings().put(getRequestAttributes().get("setting").toString(), value.get("value"));
        return DEFAULT_SUCCESS;
    }

    @Delete
    public JacksonRepresentation delete() {
        User user = getUserFromRequest();
        if (user == null) return clientError("NO_SUCH_USER", Status.CLIENT_ERROR_BAD_REQUEST);
        return new JacksonRepresentation(user.settings().remove(getRequestAttributes().get("setting").toString()));
    }
}
