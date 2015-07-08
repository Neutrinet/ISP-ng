package be.neutrinet.ispng.util;

import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by wannes on 7/6/15.
 * Best class name *ever*
 */
public class AuthenticationMigrationAutomationIntegration {
    private static HashMap<String, String> passwords = new HashMap<>();

    /**
     * Checks password is present in LDAP, if not try to verify it using old password hash (bcrypt).
     * If valid, set password in LDAP and proceed
     *
     * @param dn
     * @param password
     */
    public static int intercept(String dn, String password) {
        Optional<String> pwdFile = Config.get("util/migration/passwordFile");
        if (!pwdFile.isPresent()) {
            return 0;
        }

        if (passwords.isEmpty()) {
            try {
                ObjectMapper om = new ObjectMapper();
                HashMap<Object, Object> val = om.readValue(new File(pwdFile.get()), HashMap.class);
                for (Map.Entry<Object, Object> entry : val.entrySet()) {
                    if (entry.getKey() instanceof String) {
                        passwords.put(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(AuthenticationMigrationAutomationIntegration.class).error("Failed to load migration password file", ex);
            }
        }

        String email = dn.substring(5, dn.indexOf(','));
        User user = Users.get(email);
        if (user != null) {
            if (user.getPassword().equals("")) {
                // Empty password found, check if old password exists, and if so, migrate
                if (passwords.containsKey(email) && BCrypt.checkpw(password, passwords.get(email))) {
                    user.setPassword(password);
                    Users.update(user);
                    return 1;
                } else {
                    Logger.getLogger(AuthenticationMigrationAutomationIntegration.class).error("User " + email + " has no password set");
                }
            } else {
                return 0;
            }
        }

        return -1;
    }
}
