package be.neutrinet.ispng.security;

import be.neutrinet.ispng.vpn.Users;

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

    public static <T extends OwnedEntity> List<T> filterAccessible(UUID user, List<T> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.parallelStream().filter(e -> INSTANCE.canAccess(user, e)).collect(Collectors.toList());
    }

    public static <T extends OwnedEntity> List<T> filterModifiable(UUID user, List<T> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.parallelStream().filter(e -> INSTANCE.canModify(user, e)).collect(Collectors.toList());
    }

    public boolean canAccess(UUID user, OwnedEntity entity) {
        return !(entity == null || user == null) && (Users.isAdmin(user) || entity.isOwnedBy(user));
    }

    public boolean canModify(UUID user, OwnedEntity entity) {
        return !(entity == null || user == null) && (Users.isAdmin(user) || entity.isOwnedBy(user));
    }
}
