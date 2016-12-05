package be.neutrinet.ispng.vpn.ca;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.api.VPNClientCertificate;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CA {

    public final static String SIGNING_ALGORITHM = "SHA512withRSA";
    private static CA instance;

    private KeyStore keyStore;
    private PrivateKey caKey;
    private X509Certificate caCert;

    private CA() {
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(VPN.cfg.getProperty("ca.keyStore")),
                    VPN.cfg.get("ca.keyStorePassword").toString().toCharArray());
            caKey = (PrivateKey) keyStore.getKey("ca", VPN.cfg.get("ca.keyPassword").toString().toCharArray());
            caCert = (X509Certificate) keyStore.getCertificate("ca");
        } catch (Exception ex) {
            Logger.getLogger(VPNClientCertificate.class).error("Failed to load ca key", ex);
        }
    }

    public static CA get() {
        if (instance == null) instance = new CA();
        return instance;
    }

    // loosely based on http://stackoverflow.com/questions/7230330/sign-csr-using-bouncy-castle
    // returns signed certificate in DER format
    public BigInteger signCSR(PKCS10CertificationRequest csr, Date expiration) throws Exception {
        try {
            // Certificate serials should be random (hash)
            //http://crypto.stackexchange.com/questions/257/unpredictability-of-x-509-serial-numbers
            SecureRandom random = new SecureRandom();
            byte[] serial = new byte[16];
            random.nextBytes(serial);
            BigInteger bigserial = new BigInteger(serial);
            bigserial = bigserial.abs();

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNING_ALGORITHM);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            // http://stackoverflow.com/questions/7567837/attributes-reversed-in-certificate-subject-and-issuer
            X500Name issuer = new JcaX509CertificateHolder((X509Certificate) caCert).getSubject();

            X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(
                    issuer,
                    bigserial,
                    new Date(),
                    expiration,
                    X500Name.getInstance(csr.getSubject()),
                    csr.getSubjectPublicKeyInfo());

            // Constraints and usage
            BasicConstraints basicConstraints = new BasicConstraints(false);
            KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature);
            ExtendedKeyUsage eku = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);

            certgen.addExtension(Extension.basicConstraints, false, basicConstraints);
            certgen.addExtension(Extension.keyUsage, false, keyUsage);
            certgen.addExtension(Extension.extendedKeyUsage, false, eku);

            // Identifiers
            BcX509ExtensionUtils extensionUtils = new BcX509ExtensionUtils();
            org.bouncycastle.asn1.x509.SubjectKeyIdentifier subjectKeyIdentifier = extensionUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo());
            AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifier(new GeneralNames
                    (new GeneralName(issuer)), caCert.getSerialNumber());

            certgen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
            certgen.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);

            ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(caKey.getEncoded()));
            X509CertificateHolder holder = certgen.build(signer);
            byte[] certencoded = holder.toASN1Structure().getEncoded();

            PemObject po = new PemObject("CERTIFICATE", certencoded);
            FileOutputStream fos = new FileOutputStream(VPN.cfg.getProperty("ca.storeDir", "ca") + "/" + bigserial.toString() + ".crt");
            JcaPEMWriter pw = new JcaPEMWriter(new OutputStreamWriter(fos));
            pw.writeObject(po);
            pw.close();

            return bigserial;
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate CSR and sign CSR", ex);
        }

        return null;
    }
}
