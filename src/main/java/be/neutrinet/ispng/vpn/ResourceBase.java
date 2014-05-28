/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn;

import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;
import org.restlet.Message;
import org.restlet.engine.header.Header;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

/**
 * Initial simple mapping
 *
 * TODO: Provide a meaningful object representation, e.g. instead of addressId
 * (int) -> address (String)
 *
 * @author double-u
 */
public abstract class ResourceBase extends ServerResource {

    private static final String HEADERS_KEY = "org.restlet.http.headers";

    protected final static JacksonRepresentation DEFAULT_ERROR = new JacksonRepresentation(new ClientError("UNKNOWN_ERROR"));

    @Post
    public Representation signCSR(Representation csr) {
        StreamRepresentation sr = (StreamRepresentation) csr;
        // Authenticate and authorize request
        // Do all kinds of security checks
        try {
            PEMReader reader = new PEMReader(sr.getReader());
            PKCS10CertificationRequest req = (PKCS10CertificationRequest) reader.readObject();
            // This makes the NSA work harder on their quantum computer
            // Require 4096 bit key
            SubjectPublicKeyInfo pkInfo = req.getCertificationRequestInfo().getSubjectPublicKeyInfo();
            RSAKeyParameters rsa = (RSAKeyParameters) PublicKeyFactory.createKey(pkInfo);
            // http://stackoverflow.com/a/20622933
            if (!(rsa.getModulus().bitLength() > 2048)) {
                ClientError err = new ClientError("ILLEGAL_KEY_SIZE");
                return new JacksonRepresentation(err);
            }

            //ExtendedKeyUsage eku = new ExtendedKeyUsage(value);
            System.out.println("");
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate CSR", ex);
        }

        return DEFAULT_ERROR;
    }

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
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Origin", "*");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Methods", "POST,OPTIONS");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Methods", "POST,OPTIONS");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Headers", "Content-Type");
        getMessageHeaders(getResponse()).add("Access-Control-Allow-Credentials", "false");
        getMessageHeaders(getResponse()).add("Access-Control-Max-Age", "60");
    }
}
