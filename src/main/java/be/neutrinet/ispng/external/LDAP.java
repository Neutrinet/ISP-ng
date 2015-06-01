package be.neutrinet.ispng.external;

import be.neutrinet.ispng.config.Config;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.controls.PasswordExpiredControl;
import com.unboundid.ldap.sdk.controls.PasswordExpiringControl;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.log4j.Logger;

import javax.net.SocketFactory;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Created by wannes on 27/05/15.
 */
public class LDAP {
    private LDAPConnection connection;
    private Logger logger = Logger.getLogger(getClass());
    private final static LDAP instance = new LDAP();

    private LDAP() {

    }

    public static LDAP get() {
        return instance;
    }

    public void boot() {
        Optional<String> host = Config.get("ldap/host");
        if (host.isPresent()) {
            try {
                SSLUtil sslUtil = new SSLUtil(null, new TrustAllTrustManager());
                SocketFactory socketFactory = sslUtil.createSSLSocketFactory();
                connection = new LDAPConnection(socketFactory, host.get(), Integer.parseInt(Config.get("ldap/port", "636")));

                Optional<String> dn = Config.get("ldap/bind/dn");
                Optional<String> password = Config.get("ldap/bind/password");

                if (!dn.isPresent() || !password.isPresent()) {
                    throw new IllegalArgumentException("LDAP bind DN and/or password not present");
                }

                BindRequest bindRequest = new SimpleBindRequest(dn.get(), password.get());
                BindResult bindResult = connection.bind(bindRequest);

                PasswordExpiredControl pwdExpired = PasswordExpiredControl.get(bindResult);
                if (pwdExpired == null) {
                    logger.debug("The password expired control was not included in " +
                            "the LDAP BIND response.");
                } else {
                    logger.error("You must change your LDAP password " +
                            "before you will be allowed to perform any other operations.");
                }

                PasswordExpiringControl pwdExpiring = PasswordExpiringControl.get(bindResult);
                if (pwdExpiring == null) {
                    logger.debug("The password expiring control was not included in" +
                            " the LDAP BIND response.");
                } else {
                    logger.warn("Your LDAP password will expire in " +
                            pwdExpiring.getSecondsUntilExpiration() + " seconds.");
                }

                logger.info("Connected to LDAP");

            } catch (Exception ex) {
                logger.error("Failed to connect to LDAP server", ex);
                System.exit(666);
            }
        }
    }

    public static String findAttributeName(Class clazz, String fieldName) {
        assert clazz != null;
        assert fieldName != null;

        try {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) {
                    LDAPField field = f.getAnnotation(LDAPField.class);
                    if (field != null) {
                        return field.attribute();
                    } else {
                        return fieldName;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(LDAP.class).error("Failed to find LDAP attribute for field " + fieldName, ex);
        }

        return fieldName;
    }

    public static LDAPConnection connection() {
        return instance.connection;
    }
}
