/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn;

import java.util.concurrent.ConcurrentMap;
import org.restlet.Message;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

/**
 * @author double-u
 */
public abstract class ResourceBase extends ServerResource {

    private static final String HEADERS_KEY = "org.restlet.http.headers";

    protected final static JacksonRepresentation DEFAULT_ERROR = new JacksonRepresentation(new ClientError("UNKNOWN_ERROR"));
    protected final static JacksonRepresentation DEFAULT_SUCCESS = new JacksonRepresentation("OK");

    @SuppressWarnings("unchecked")
    static Series<Header> getMessageHeaders(Message message) {
        ConcurrentMap<String, Object> attrs = message.getAttributes();
        Series<Header> headers = (Series<Header>) attrs.get(HEADERS_KEY);
        if (headers == null) {
            headers = new Series<>(Header.class);
            Series<Header> prev = (Series<Header>) attrs.putIfAbsent(HEADERS_KEY, headers);
            if (prev != null) {
                headers = prev;
            }
        }
        return headers;
    }

    protected JacksonRepresentation error() {
        setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return DEFAULT_ERROR;
    }

    protected JacksonRepresentation clientError(String key, Status status) {
        setStatus(status);
        return new JacksonRepresentation(new ClientError(key));
    }

    protected void setCORSHeaders(Representation entity) {
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Origin", "*");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Methods", "POST,OPTIONS");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Methods", "POST,OPTIONS");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Headers", "Content-Type");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Credentials", "false");
        getMessageHeaders(getResponse()).add("Access-Control-Max-Age", "60");
    }

    @Options
    public void doOptions(Representation entity) {
        setCORSHeaders(entity);
    }
}
