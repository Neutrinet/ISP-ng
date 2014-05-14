/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.Connections;
import be.neutrinet.ispng.vpn.ca.CSRUtils;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * Initial simple mapping
 *
 * TODO: Provide a meaningful object representation, e.g. instead of addressId
 * (int) -> address (String)
 *
 * @author double-u
 */
public class API extends ServerResource {

    @Get
    public List<Connection> activeConnections() throws SQLException {
        return Connections.dao.queryForEq("active", "1");
    }

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

        return new JacksonRepresentation(new ClientError("UNKNOWN_ERROR"));
    }
}
