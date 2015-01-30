/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.SessionToken;
import be.neutrinet.ispng.security.SessionTokens;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.User;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsResponseHelper;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author wannes
 */
public abstract class ResourceBase extends ServerResource {

    protected final static JacksonRepresentation DEFAULT_ERROR = new JacksonRepresentation(new ClientError("UNKNOWN_ERROR"));
    protected final static JacksonRepresentation DEFAULT_SUCCESS = new JacksonRepresentation("OK");
    protected SessionToken token;

    protected JacksonRepresentation error() {
        setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return DEFAULT_ERROR;
    }

    protected JacksonRepresentation clientError(String key, Status status) {
        setStatus(status);
        return new JacksonRepresentation(new ClientError(key));
    }

    protected void setCORSHeaders(Representation entity) {
        getResponse().setAccessControlAllowOrigin("*");
    }

    protected <T> Optional<T> mergeUpdate(T original, T updated) {
        return this.mergeUpdate(original, updated, new ArrayList<>());
    }

    protected <T> Optional<T> mergeUpdate(T original, T updated, List<String> prohibitedFields) {
        if (original == null || updated == null) throw new IllegalArgumentException("Not all arguments were present");
        if (prohibitedFields == null) prohibitedFields = new ArrayList<>();

        for (Field f : original.getClass().getFields()) {
            // Never update ID fields
            if (f.getName().equals("id") || prohibitedFields.contains(f.getName())) continue;

            f.setAccessible(true);

            try {
                Object newValue = f.get(updated);
                if (newValue == null) continue;
                f.set(original, newValue);
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("Failed to merge updates");
                return Optional.empty();
            }

        }

        return Optional.of(original);
    }

    @Options
    public void doOptions(Representation entity) {
        CorsResponseHelper corsResponseHelper = new CorsResponseHelper();
        corsResponseHelper.allowedCredentials = true;
        corsResponseHelper.allowAllRequestedHeaders = true;
        corsResponseHelper.addCorsResponseHeaders(getRequest(), getResponse());
        //setCORSHeaders(entity);
    }

    public Optional<SessionToken> getSessionToken() {
        if (token != null) return Optional.of(token);

        String sessionToken = null;
        if (getRequest().getCookies().getFirst("Session") != null) {
            sessionToken = getRequest().getCookies().getFirstValue("Session");
        } else if (getRequest().getHeaders().getFirstValue("Session") != null) {
            sessionToken = getRequest().getHeaders().getFirstValue("Session");
        }

        if (sessionToken != null) {
            try {
                token = SessionTokens.dao.queryForEq("token", UUID.fromString(sessionToken)).get(0);
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("Failed to query for session token", ex);
            }
        }

        return Optional.ofNullable(token);
    }

    public User getLoggedInUser() {
        return getSessionToken().get().getUser();
    }
}
