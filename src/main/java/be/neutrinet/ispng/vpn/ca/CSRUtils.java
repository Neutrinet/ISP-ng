/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.ca;

import java.security.cert.CertificateException;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;

/**
 *
 * @author double-u
 */
public class CSRUtils {

    // Thank god for this blog post
    // TODO : Author did not provide proper license info, should ask
    // http://unitstep.net/blog/2008/10/27/extracting-x509-extensions-from-a-csr-using-the-bouncy-castle-apis/
    public static X509Extensions getExtensions(final PKCS10CertificationRequest certificateSigningRequest) throws CertificateException {
        final CertificationRequestInfo certificationRequestInfo = certificateSigningRequest
                .getCertificationRequestInfo();

        final ASN1Set attributesAsn1Set = certificationRequestInfo.getAttributes();

        // The `Extension Request` attribute is contained within an ASN.1 Set,
        // usually as the first element.
        X509Extensions certificateRequestExtensions = null;
        for (int i = 0; i < attributesAsn1Set.size(); ++i) {
            // There should be only only one attribute in the set. (that is, only
            // the `Extension Request`, but loop through to find it properly)
            final DEREncodable derEncodable = attributesAsn1Set.getObjectAt(i);
            if (derEncodable instanceof DERSequence) {
                final Attribute attribute = new Attribute((DERSequence) attributesAsn1Set
                        .getObjectAt(i));

                if (attribute.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                    // The `Extension Request` attribute is present.
                    final ASN1Set attributeValues = attribute.getAttrValues();

                    // The X509Extensions are contained as a value of the ASN.1 Set.
                    // Assume that it is the first value of the set.
                    if (attributeValues.size() >= 1) {
                        certificateRequestExtensions = new X509Extensions((ASN1Sequence) attributeValues
                                .getObjectAt(0));

                        // No need to search any more.
                        break;
                    }
                }
            }
        }

        return certificateRequestExtensions;
    }
}
