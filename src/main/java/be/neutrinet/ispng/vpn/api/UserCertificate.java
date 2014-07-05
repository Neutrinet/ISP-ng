/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import java.io.File;
import java.io.FileWriter;

import be.neutrinet.ispng.vpn.ca.Certificate;
import be.neutrinet.ispng.vpn.ca.Certificates;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Put;

/**
 * 
 * @author wannes
 */
public class UserCertificate extends ResourceBase {


    @Put 
    public Representation signCSR(Representation csrstream) {
        if (!getRequest().getAttributes().containsKey("user")) {
            return DEFAULT_ERROR;
        }
        
        StreamRepresentation sr = (StreamRepresentation) csrstream;

        // Do all kinds of security checks
        try {
            User user = Users.dao.queryForId(getRequest().getAttributes().get("user").toString());
            
            PEMParser parser = new PEMParser(sr.getReader());
            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) parser.readObject();
            // This makes the NSA work harder on their quantum computer
            // Require 4096 bit key
            SubjectPublicKeyInfo pkInfo = csr.getSubjectPublicKeyInfo();
            RSAKeyParameters rsa = (RSAKeyParameters) PublicKeyFactory.createKey(pkInfo);
            // http://stackoverflow.com/a/20622933
            if (!(rsa.getModulus().bitLength() > 2048)) {
                ClientError err = new ClientError("ILLEGAL_KEY_SIZE");
                return new JacksonRepresentation(err);
            }
            
            String caStorePath = VPN.cfg.getProperty("ca.storeDir", "ca");
            File dir = new File(caStorePath);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }

            Certificate cert = new Certificate();
            cert.user = user;
            Certificates.dao.create(cert);

            FileWriter fw = new FileWriter(caStorePath + "/" + cert.id + ".csr");
            PEMWriter pw = new PEMWriter(fw);
            pw.writeObject(csr);
            pw.flush();
            
            return DEFAULT_SUCCESS;
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate CSR and/or sign CSR", ex);
        }

        return DEFAULT_ERROR;
    }
}
