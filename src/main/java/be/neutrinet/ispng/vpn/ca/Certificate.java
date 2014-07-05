package be.neutrinet.ispng.vpn.ca;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.User;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Created by double-u on 04/07/14.
 */
@DatabaseTable(tableName = "certificates")
public class Certificate {

    @DatabaseField(allowGeneratedIdInsert = true)
    public int id;

    @DatabaseField(foreign = true, canBeNull = false)
    public User user;

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

    public X509Certificate get() {
        String crtPath = VPN.cfg.getProperty("ca.storeDir", "ca") + "/" + serial + ".crt";
        File crt = new File(crtPath);

        try {
            if (crt.exists()) {
                PEMParser pp = new PEMParser(new FileReader(crt));
                return (X509Certificate) pp.readObject();
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to load certificate", ex);
        }

        return null;
    }
}
