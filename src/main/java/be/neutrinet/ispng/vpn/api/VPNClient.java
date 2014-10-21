package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.Clients;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import java.util.List;

/**
 * Created by wannes on 11/10/14.
 */
public class VPNClient extends ResourceBase {
    @Post
    public void modifyVPNClient(Client client) {

    }

    @Put
    public Representation addVPNClient(Client client) {
        if ((client.commonName == null || client.commonName.isEmpty()) ||
                (client.user == null)) {
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
        String userId = getRequestAttributes().get("user").toString();
        if (userId == null || userId.isEmpty()) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            List<Client> clients = Clients.dao.queryForEq("user_id", userId);
            return new JacksonRepresentation<>(clients);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve clients", ex);
        }

        return clientError("UNKNOWN_ERROR", Status.SERVER_ERROR_INTERNAL);
    }
}
