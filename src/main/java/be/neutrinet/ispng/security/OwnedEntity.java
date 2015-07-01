package be.neutrinet.ispng.security;

import java.util.UUID;

/**
 * Created by wannes on 12/20/14.
 */
public interface OwnedEntity {
    public boolean isOwnedBy(UUID user);
}
