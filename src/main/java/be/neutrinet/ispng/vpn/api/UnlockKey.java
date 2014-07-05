/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.admin.UnlockKeys;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 *
 * @author wannes
 */
public class UnlockKey extends ResourceBase {

    private final SecureRandom random = new SecureRandom();

    public String generateUnlockKey() {
        return new BigInteger(130, random).toString(32);
    }

    @Get
    public Representation getKeys() {
        try {
            List<be.neutrinet.ispng.vpn.admin.UnlockKey> keys = UnlockKeys.dao.queryForAll();
            return new JacksonRepresentation<>(keys);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to get unlock keys", ex);
            return DEFAULT_ERROR;
        }
    }

    @Put
    public Representation addUnlockKey(Map<String, String> data) {
        if (!data.containsKey("email")) {
            throw new IllegalArgumentException("No email address given");
        }
        try {
            be.neutrinet.ispng.vpn.admin.UnlockKey key = new be.neutrinet.ispng.vpn.admin.UnlockKey();
            key.email = data.get("email");
            key.key = generateUnlockKey();
            UnlockKeys.dao.createIfNotExists(key);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to add unlock key", ex);
            return DEFAULT_ERROR;
        }
        return DEFAULT_SUCCESS;
    }

}
