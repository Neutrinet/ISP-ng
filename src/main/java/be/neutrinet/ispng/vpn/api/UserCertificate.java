/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.ResourceBase;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StreamRepresentation;
import org.restlet.resource.Put;

/**
 * 
 * @author wannes
 */
public class UserCertificate extends ResourceBase {

    private static KeyStore keyStore;
    private static PrivateKey caKey;
    private static X509Certificate caCert;

    static {
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(VPN.cfg.getProperty("ca.keyStore")),
                    VPN.cfg.get("ca.keyStorePassword").toString().toCharArray());
            caKey = (PrivateKey) keyStore.getKey("cakey", VPN.cfg.get("ca.keyPassword").toString().toCharArray());
            caCert = (X509Certificate) keyStore.getCertificate("cacert");
        } catch (Exception ex) {
            Logger.getLogger(UserCertificate.class).error("Failed to load ca key", ex);
        }
    }

    @Put // loosely based on http://stackoverflow.com/questions/7230330/sign-csr-using-bouncy-castle
    public Representation signCSR(Representation csrstream) {
        if (caKey == null) return error();
        
        StreamRepresentation sr = (StreamRepresentation) csrstream;

        // Do all kinds of security checks
        try {
            PEMReader reader = new PEMReader(sr.getReader());
            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) reader.readObject();
            CertificationRequestInfo csri = csr.getCertificationRequestInfo();
            // This makes the NSA work harder on their quantum computer
            // Require 4096 bit key
            SubjectPublicKeyInfo pkInfo = csr.getCertificationRequestInfo().getSubjectPublicKeyInfo();
            RSAKeyParameters rsa = (RSAKeyParameters) PublicKeyFactory.createKey(pkInfo);
            // http://stackoverflow.com/a/20622933
            if (!(rsa.getModulus().bitLength() > 2048)) {
                ClientError err = new ClientError("ILLEGAL_KEY_SIZE");
                return new JacksonRepresentation(err);
            }
            
            // Certificate serials should be random (hash)
            //http://crypto.stackexchange.com/questions/257/unpredictability-of-x-509-serial-numbers
            SecureRandom random = new SecureRandom();
            byte[] serial = new byte[16];
            random.nextBytes(serial);
            BigInteger bigserial = new BigInteger(serial);

            // One year certificate validity
            Instant expirationDate = Instant.now();
            expirationDate.plus(1, ChronoUnit.YEARS);
            
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            
            X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(
                    new X500Name("C=BE, ST=Brussels Capital Region, L=Brussels, "
                            + "O=Neutrinet, OU=CA, CN=ca.neutrinet.be/"
                            + "name=Neutrinet User Certificate Authority/"
                            + "emailAddress=contact@neutrinet.be"), 
                    bigserial,
                    new Date(),
                    Date.from(expirationDate),
                    X500Name.getInstance(csri.getSubject()),
                    csri.getSubjectPublicKeyInfo());
            
            // Constraints and usage
            BasicConstraints basicConstraints = new BasicConstraints(false);
            ExtendedKeyUsage eku = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);
            
            certgen.addExtension(X509Extension.basicConstraints, false, basicConstraints);
            certgen.addExtension(X509Extension.extendedKeyUsage, false, eku);
            
            // Identifiers
            SubjectKeyIdentifier subjectKeyIdentifier = new SubjectKeyIdentifier(csri.getSubjectPublicKeyInfo());
            AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifier(new GeneralNames
                (new GeneralName(new X500Name(caCert.getSubjectX500Principal().getName()))), caCert.getSerialNumber());
            
            certgen.addExtension(X509Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
            certgen.addExtension(X509Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
            
            ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(caKey.getEncoded()));
            X509CertificateHolder holder = certgen.build(signer);
            byte[] certencoded = holder.toASN1Structure().getEncoded();

            return new ByteArrayRepresentation(certencoded);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate CSR and sign CSR", ex);
        }

        return DEFAULT_ERROR;
    }
}
