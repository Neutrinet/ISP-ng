package be.neutrinet.ispng.security;

import be.neutrinet.ispng.vpn.User;

/**
 * Created by wannes on 12/20/14.
 */
public interface OwnedEntity {
    public boolean isOwnedBy(User user);
}
