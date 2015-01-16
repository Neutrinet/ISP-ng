package be.neutrinet.ispng.security;

import be.neutrinet.ispng.vpn.User;
import org.apache.log4j.Logger;

import java.util.UUID;

/**
 * Created by wannes on 12/18/14.
 */
public class SessionManager {
    private static SessionManager instance = new SessionManager();

    public static SessionToken createSessionToken(User user, String address) {
        return new SessionToken(user, address);
    }

    public static boolean validateToken(String token, String address) {
        if (token != null && !token.isEmpty()) {
            try {
                UUID id = UUID.fromString(token);
                SessionToken sessionToken = SessionTokens.dao.queryForEq("token", id).get(0);
                return sessionToken != null && sessionToken.valid() && sessionToken.getAddress().equals(address);
            } catch (Exception ex) {
                Logger.getLogger(SessionManager.class).error("Failed to validate token", ex);
            }
        }

        return false;
    }
}
