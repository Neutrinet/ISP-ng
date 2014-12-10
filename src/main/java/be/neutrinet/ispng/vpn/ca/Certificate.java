package be.neutrinet.ispng.vpn.ca;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.Client;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.Date;

/**
 * Created by wannes on 04/07/14.
 */
@DatabaseTable(tableName = "certificates")
public class Certificate {

    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    public Client client;

    @DatabaseField
    public BigInteger serial;

    @DatabaseField
    public Date signedDate;

    @DatabaseField
    public Date revocationDate;

    public PKCS10CertificationRequest loadRequest() {
        String csrPath = VPN.cfg.getProperty("ca.storeDir", "ca") + "/" + id + ".csr";
        File csr = new File(csrPath);

        try {
            if (csr.exists()) {
                PEMParser pp = new PEMParser(new FileReader(csr));
                return (PKCS10CertificationRequest) pp.readObject();
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to load csr", ex);
        }

        return null;
    }

    public X509CertificateHolder get() {
        String crtPath = VPN.cfg.getProperty("ca.storeDir", "ca") + "/" + serial + ".crt";
        File crt = new File(crtPath);

        try {
            if (crt.exists()) {
                PEMParser pp = new PEMParser(new FileReader(crt));
                return (X509CertificateHolder) pp.readObject();
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to load certificate", ex);
        }

        return null;
    }
    
    public byte[] getRaw() {
        String crtPath = VPN.cfg.getProperty("ca.storeDir", "ca") + "/" + serial + ".crt";
        File crt = new File(crtPath);

        try {
            if (crt.exists()) {
                return IOUtils.toByteArray(new FileInputStream(crt));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to load certificate", ex);
        }

        return null;
    }
}
