/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.Clients;
import be.neutrinet.ispng.vpn.ca.CA;
import be.neutrinet.ispng.vpn.ca.Certificate;
import be.neutrinet.ispng.vpn.ca.Certificates;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 *
 * @author wannes
 */
public class VPNClientCertificate extends ResourceBase {

    public final static MediaType PEM_MIME = new MediaType("application/x-pem-file", "PEM encoded object");

    public static X509CertificateHolder sign(Certificate cert) {
        try {
            // One year certificate validity
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(365);
            Date expiration = DateUtil.convert(expirationDate);

            cert.signedDate = new Date();
            cert.revocationDate = expiration;

            cert.serial = CA.get().signCSR(cert.loadRequest(), expiration);

            Certificates.dao.update(cert);

            return cert.get();
        } catch (Exception ex) {
            Logger.getLogger(VPNClientCertificate.class).error("Failed to sign certificate", ex);
        }

        return null;
    }

    @Get
    public Representation getCertificate() {
        // TODO: decide if returning an entire list of certificates needs to be implemented
        if (!getRequestAttributes().containsKey("client")) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        String clientId = getAttribute("client").toString();
        try {
            List<Certificate> certs = Certificates.dao.queryForEq("client_id", clientId);

            if (getQueryValue("raw") != null) {
                if (getRequestAttributes().containsKey("cert") && !getAttribute("cert").equals("all")) {
                    String certId = getAttribute("cert").toString();

                    Certificate cert = certs.stream()
                            .filter(c -> c.id == Integer.parseInt(certId))
                            .iterator()
                            .next();

                    X509CertificateHolder c = null;
                    if (cert.signedDate == null) {
                        c = sign(cert);
                    } else {
                        c = cert.get();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(baos);
                    PemObject po = new PemObject("CERTIFICATE", c.getEncoded());
                    PEMWriter pw = new PEMWriter(osw);
                    pw.writeObject(po);
                    pw.close();


                    return new ByteArrayRepresentation(baos.toByteArray(), PEM_MIME);
                } else {
                    return clientError("ONE_RAW_CERT", Status.CLIENT_ERROR_NOT_ACCEPTABLE);
                }
            } else {
                if (getRequestAttributes().containsKey("cert") && !getAttribute("cert").equals("all")) {
                    String certId = getAttribute("cert").toString();

                    Certificate cert = certs.stream()
                            .filter(c -> c.id == Integer.parseInt(certId))
                            .iterator()
                            .next();

                    return new JacksonRepresentation(cert);
                } else {
                    return new JacksonRepresentation(certs);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(VPNClientCertificate.class).error("Failed to get certificate", ex);
        }

        return DEFAULT_ERROR;
    }

    @Put()
    public Representation storeCSR(Representation csrstream) {
        if (!getRequest().getAttributes().containsKey("client")) {
            return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        StreamRepresentation sr = (StreamRepresentation) csrstream;

        // Do all kinds of security checks
        try {
            Client client = Clients.dao.queryForId(getAttribute("client").toString());

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


            X500Name subject = X500Name.getInstance(csr.getSubject());
            RDN[] rdns = subject.getRDNs(BCStyle.CN);
            if (rdns == null || rdns.length == 0) {
                return clientError("NO_CSR_CN", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            String CN = IETFUtils.valueToString(rdns[0].getFirst().getValue());
            if (CN == null || CN.isEmpty()) {
                return clientError("INVALID_CSR_CN", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            // couple CN to client
            client.commonName = CN;
            Clients.dao.update(client);

            String caStorePath = VPN.cfg.getProperty("ca.storeDir", "ca");
            File dir = new File(caStorePath);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }

            Certificate cert = new Certificate();
            cert.client = client;
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
