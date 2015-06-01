package be.neutrinet.ispng.security;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by wannes on 12/20/14.
 */
public class Policy {
    private final static Policy INSTANCE = new Policy();

    public final static Policy get() {
        return INSTANCE;
    }

    public static <T extends OwnedEntity> List<T> filterAccessible(User user, List<T> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.parallelStream().filter(e -> INSTANCE.canAccess(user, e)).collect(Collectors.toList());
    }

    public static <T extends OwnedEntity> List<T> filterModifiable(User user, List<T> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.parallelStream().filter(e -> INSTANCE.canModify(user, e)).collect(Collectors.toList());
    }

    public boolean canAccess(User user, OwnedEntity entity) {
        if (entity == null || user == null) return false;
        if (isAdmin(user)) return true;
        if (entity.isOwnedBy(user)) return true;

        return false;
    }

    public boolean canModify(User user, OwnedEntity entity) {
        if (entity == null || user == null) return false;
        if (isAdmin(user)) return true;
        if (entity.isOwnedBy(user)) return true;

        return false;
    }

    public final boolean isAdmin(User user) {
        if (VPN.cfg.getProperty("users.admin") == null) return false;

        boolean match = false;
        String[] str = VPN.cfg.getProperty("users.admin").split(";");
        for (String s : str) {
            if (user.globalId.equals(UUID.fromString(s))) {
                match = true;
            }
        }

        return match;
    }

    public final boolean isRelatedService(User user) {
        if (VPN.cfg.getProperty("users.service") == null) return false;

        boolean match = false;
        String[] str = VPN.cfg.getProperty("users.service").split(";");
        for (String s : str) {
            if (user.globalId.equals(UUID.fromString(s))) {
                match = true;
            }
        }

        return match;
    }
}
