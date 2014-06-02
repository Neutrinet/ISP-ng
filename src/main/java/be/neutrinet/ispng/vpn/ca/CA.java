package be.neutrinet.ispng.vpn.ca;

import java.security.*;
import java.io.*;
import java.util.Date;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x500.*;
import org.bouncycastle.asn1.pkcs.*;
import org.bouncycastle.openssl.*;
import org.bouncycastle.pkcs.*;
import org.bouncycastle.cert.*;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.crypto.util.*;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.bc.*;
import org.bouncycastle.operator.jcajce.*;
import org.bouncycastle.util.encoders.Base64;

public class CA {

    public final static String SIGNING_ALGO = "SHA512withRSA";
    public final static int KEY_SIZE = 4096;

    /**
     * http://stackoverflow.com/questions/7230330/sign-csr-using-bouncy-castle
     * Given a Keystore containing a private key and certificate and a Reader
     * containing a PEM-encoded Certificiate Signing Request (CSR), sign the CSR
     * with that private key and return the signed certificate as a PEM-encoded
     * PKCS#7 signedData object. The returned value can be written to a file and
     * imported into a Java KeyStore with "keytool -import -trustcacerts -alias
     * subjectalias -file file.pem"
     *
     * @param pemcsr a Reader from which will be read a PEM-encoded CSR (begins
     * "-----BEGIN NEW CERTIFICATE REQUEST-----")
     * @param validity the number of days to sign the Certificate for
     * @param keystore the KeyStore containing the CA signing key
     * @param alias the alias of the CA signing key in the KeyStore
     * @param password the password of the CA signing key in the KeyStore
     *
     * @return a String containing the PEM-encoded signed Certificate (begins
     * "-----BEGIN PKCS #7 SIGNED DATA-----")
     */
    protected String signCSR(Reader pemcsr, int validity, KeyStore keystore, String alias, char[] password) throws Exception {
        PrivateKey cakey = (PrivateKey) keystore.getKey(alias, password);
        X509Certificate cacert = (X509Certificate) keystore.getCertificate(alias);
        PEMReader reader = new PEMReader(pemcsr);
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest((CertificationRequest) reader.readObject());

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNING_ALGO);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        X500Name issuer = new X500Name(cacert.getSubjectX500Principal().getName());
        BigInteger serial = new BigInteger(32, new SecureRandom());
        Date from = new Date();
        Date to = new Date(System.currentTimeMillis() + (validity * 86400000L));

        X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(issuer, serial, from, to, csr.getSubject(), csr.getSubjectPublicKeyInfo());
        certgen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
        certgen.addExtension(X509Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));
        certgen.addExtension(X509Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(new GeneralNames(new GeneralName(new X509Name(cacert.getSubjectX500Principal().getName()))), cacert.getSerialNumber()));

        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(cakey.getEncoded()));
        X509CertificateHolder holder = certgen.build(signer);
        byte[] certencoded = holder.toASN1Structure().getEncoded();

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        signer = new JcaContentSignerBuilder(SIGNING_ALGO).build(cakey);
        generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(signer, cacert));
        generator.addCertificate(new X509CertificateHolder(certencoded));
        generator.addCertificate(new X509CertificateHolder(cacert.getEncoded()));
        CMSTypedData content = new CMSProcessableByteArray(certencoded);
        CMSSignedData signeddata = generator.generate(content, true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("-----BEGIN PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
        out.write(Base64.encode(signeddata.getEncoded()));
        out.write("\n-----END PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
        out.close();
        return new String(out.toByteArray(), "ISO-8859-1");
    }
}
