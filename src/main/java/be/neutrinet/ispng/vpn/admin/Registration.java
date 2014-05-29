/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.admin;

import be.neutrinet.ispng.vpn.User;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author wannes
 */
public class Registration {

    private final UUID id;
    public User user;
    public long timeInitiated;
    public int IPv4Id;
    public int IPv6Id;

    private static final Map<UUID, Registration> activeRegistrations = new HashMap<>();

    public static Map<UUID, Registration> getActiveRegistrations() {
        return activeRegistrations;
    }

    public Registration(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

}
