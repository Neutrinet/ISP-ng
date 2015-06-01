package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.Policy;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.Clients;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import java.util.List;

/**
 * Created by wannes on 11/10/14.
 */
public class VPNClient extends ResourceBase {
    @Post
    public Representation modifyVPNClient(Client client) {
        if (!getRequestAttributes().containsKey("client"))
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

        try {
            Client c = Clients.dao.queryForId(getAttribute("client"));
            if (!Policy.get().canModify(getSessionToken().get().getUser(), c)) {
                return clientError("FORBIDDEN", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            if (c != null) {
                c = mergeUpdate(c, client).get();
            }
            Clients.dao.update(c);
            return new JacksonRepresentation<>(client);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to update client", ex);
        }

        return clientError("UNKNOWN_ERROR", Status.SERVER_ERROR_INTERNAL);
    }

    @Put
    public Representation addVPNClient(Client client) {
        if ((client.commonName == null || client.commonName.isEmpty()) ||
                (client.userId == null)) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            Clients.dao.create(client);

            return new JacksonRepresentation<>(client);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve clients", ex);
        }

        return clientError("UNKNOWN_ERROR", Status.SERVER_ERROR_INTERNAL);
    }

    @Get
    public Representation getClients() {
        try {
            if (getRequestAttributes().containsKey("client"))
                if (!getAttribute("client").toLowerCase().equals("all"))
                return new JacksonRepresentation<>(Clients.dao.queryForId(getAttribute("client")));

            List<Client> clients;

            if (getQueryValue("user") != null)
                clients = Clients.dao.queryForEq("user_id", getQueryValue("user"));
            else
                clients = Clients.dao.queryForAll();

            clients = Policy.filterAccessible(getSessionToken().get().getUser(), clients);
            return new JacksonRepresentation<>(clients);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve clients", ex);
        }

        return clientError("UNKNOWN_ERROR", Status.SERVER_ERROR_INTERNAL);
    }

    @Delete
    public Representation delete() {
        try {
            if (!getRequestAttributes().containsKey("client"))
                return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

            Client c = Clients.dao.queryForId(getAttribute("client"));
            if (!Policy.get().canModify(getSessionToken().get().getUser(), c)) {
                return clientError("FORBIDDEN", Status.CLIENT_ERROR_BAD_REQUEST);
            }
            Clients.dao.delete(c);

            return DEFAULT_SUCCESS;
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to delete client", ex);
        }

        return clientError("UNKNOWN_ERROR", Status.SERVER_ERROR_INTERNAL);
    }
}
