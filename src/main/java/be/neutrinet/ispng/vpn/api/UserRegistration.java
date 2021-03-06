/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.security.SessionManager;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.Clients;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.Registrations;
import be.neutrinet.ispng.vpn.admin.UnlockKey;
import be.neutrinet.ispng.vpn.admin.UnlockKeys;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author wannes
 */
public class UserRegistration extends ResourceBase {

    public static SimpleDateFormat EUROPEAN_DATE_FORMAT = new SimpleDateFormat("dd-mm-yyyy");

    @Get
    public Representation handleGet() {
        String lastSegment = getReference().getLastSegment();

        if (lastSegment != null) {
            UUID id = UUID.fromString(lastSegment);
            if (id == null) {
                return error();
            }

            Registration reg = Registration.getActiveRegistrations().get(id);
            if (reg == null) {
                try {
                    // try fetch from db
                    List<Registration> results = Registrations.dao.queryForEq("id", id);
                    if (results.size() == 1) {
                        reg = results.get(0);
                    } else {
                        return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    // Legacy fix
                    if (reg.client == null) {
                        reg.client = Clients.dao.queryForEq("userId", reg.user).get(0);
                    }

                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).error("Failed to query for existing registration", ex);
                }
            }
            return new JacksonRepresentation(reg);
        }

        return error();
    }

    @Post
    public Representation handlePost(Map<String, Object> data) {
        setCORSHeaders(getResponseEntity());
        try {
            if (data.get("id") != null) {
                return handleFlow(data);
            }

            UnlockKey unlockKey = null;

            if (Config.get("vpn/registration/requireUnlockKey", "no").equals("yes")) {
                String key = (String) data.get("key");
                List<UnlockKey> keys = UnlockKeys.dao.queryForEq("key", key);
                assert keys.size() <= 1;
                if (keys.isEmpty() || keys.get(0).usedAt != null) {
                    return new JacksonRepresentation<>(new ClientError("INVALID_UNLOCK_KEY"));
                } else {
                    unlockKey = keys.get(0);
                }
            }

            Registration reg = new Registration(UUID.randomUUID());
            reg.timeInitiated = new Date();
            reg.setUser(new User());
            reg.user().email = (String) data.get("email");
            reg.unlockKey = unlockKey;

            if (data.containsKey("password")) {
                String password = (String) data.get("password");
                reg.user().setPassword(password);
            }

            Registration.getActiveRegistrations().put(reg.getId(), reg);
            Registrations.dao.create(reg);
            return new JacksonRepresentation<>(reg);

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to validate unlock key", ex);
        }

        return error();
    }

    private Representation handleFlow(Map<String, Object> data) {
        if (!data.containsKey("id")) {
            return error();
        }

        UUID id = UUID.fromString((String) data.get("id"));
        Registration reg = Registration.getActiveRegistrations().get(id);

        if (reg == null) {
            return clientError("REGISTRATION_EXPIRED", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            if (data.containsKey("user")) {
                // finalize registration
                if (data.containsKey("ipv4Id"))
                    reg.ipv4Id = (int) data.get("ipv4Id");
                if (data.containsKey("ipv6Id"))
                    reg.ipv6Id = (int) data.get("ipv6Id");

                boolean sendEmailConfirmation = true;
                if (data.containsKey("sendEmail")) {
                    sendEmailConfirmation = Boolean.parseBoolean(data.get("sendEmail").toString());
                }

                reg.commit(sendEmailConfirmation);
                Registration.getActiveRegistrations().remove(reg.getId());
                return new JacksonRepresentation("OK");
            } else if (data.containsKey("password")) {
                String password = (String) data.get("password");
                reg.user().setPassword(password);
                return new JacksonRepresentation(reg.user);
            } else if (data.containsKey("name")) {
                User user = reg.user();
                user.name = (String) data.get("name");
                user.lastName = (String) data.get("last-name");
                user.birthDate = EUROPEAN_DATE_FORMAT.parse((String) data.get("birthdate"));
                user.street = (String) data.get("street");
                user.municipality = (String) data.get("municipality");
                user.postalCode = (String) data.get("postal-code");
                user.birthPlace = (String) data.get("birthplace");
                user.country = (String) data.get("country");

                try {
                    if (user.validate()) Users.add(user);
                } catch (LDAPPersistException ex) {
                    if (ex.getMessage().contains("already exists")) {
                        setStatus(Status.REDIRECTION_SEE_OTHER);
                        return new JacksonRepresentation<>(Users.get(user.email));
                    }
                }

                // Auto-login newly created user
                getResponse().getCookieSettings().add("Session",
                        SessionManager.createSessionToken(user, getClientInfo().getAddress()).getToken().toString());

                reg.createInitialClient();
                Registrations.dao.update(reg);

                return new JacksonRepresentation(reg);
            }
        } catch (ParseException ex) {
            return clientError("INVALID_DATE_FORMAT", Status.CLIENT_ERROR_BAD_REQUEST);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to handle flow", ex);
        }
        return error();
    }
}
