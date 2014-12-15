package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.Connection;
import be.neutrinet.ispng.vpn.Connections;
import be.neutrinet.ispng.vpn.Manager;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;

import java.util.List;

/**
 * Created by wannes on 9/4/14.
 */
public class VPNConnections extends ResourceBase {
    @Get
    public Representation getConnection() {
        if (!getRequestAttributes().containsKey("client"))
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        String clientId = getAttribute("client").toString();
        String connectionId = getAttribute("id").toString();
        if (connectionId == null || connectionId.isEmpty()) {
            connectionId = "all";
        }

        try {
            if (clientId.equals("all")) {
                if (connectionId.equals("all"))
                    return new JacksonRepresentation(Connections.dao.queryForAll());
                else
                    return new JacksonRepresentation(Connections.dao.queryForId(connectionId));
            } else {
                if (connectionId.equals("all"))
                    return new JacksonRepresentation(Connections.dao.queryForEq("client_id", clientId));
                else
                    return new JacksonRepresentation(Connections.dao.queryForId(connectionId));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to get connection", ex);
            return clientError("INVALID_ARGUMENT", Status.SERVER_ERROR_INTERNAL);
        }
    }

    @Delete
    public Representation removeConnection() {
        String connectionId = getRequestAttributes().get("id").toString();
        if (connectionId == null || connectionId.isEmpty()) {
            return clientError("INVALID_INPUT", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            List<Connection> connections = Connections.dao.queryForEq("id", connectionId);
            for (Connection c : connections) {
                if (c.active) {
                    Manager.get().dropConnection(c);
                }

                Connections.dao.delete(c);
            }
            return DEFAULT_SUCCESS;
        } catch (Exception ex) {
            return clientError("INVALID_INPUT", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }
}
