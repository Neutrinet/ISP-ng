package be.neutrinet.ispng.security;

import be.neutrinet.ispng.vpn.User;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by wannes on 12/20/14.
 */
@DatabaseTable(tableName = "session_tokens")
public class SessionToken {
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private User user;
    @DatabaseField(canBeNull = false)
    private String address;
    @DatabaseField(canBeNull = false)
    private long creationTime;
    @DatabaseField(id = true)
    private UUID token;

    private SessionToken() {

    }

    public SessionToken(User user, String address) {
        this.user = user;
        this.address = address;
        this.creationTime = System.currentTimeMillis();
        this.token = UUID.randomUUID();

        try {
            SessionTokens.dao.create(this);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create session token", ex);
        }
    }

    public UUID getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public String getAddress() {
        return address;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean valid() {
        if (System.currentTimeMillis() - this.creationTime > 360000) return false;

        return true;
    }
}
