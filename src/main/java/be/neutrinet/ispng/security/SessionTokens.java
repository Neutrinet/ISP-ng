package be.neutrinet.ispng.security;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Created by wannes on 12/20/14.
 */
public class SessionTokens {
    public static Dao<SessionToken, String> dao;

    static {
        Class cls = SessionToken.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);
            deleteExpiredSessions();
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);

        }
    }

    public static void deleteExpiredSessions() {
        try {
            DeleteBuilder<SessionToken, String> db = dao.deleteBuilder();
            db.where().lt("creationTime", System.currentTimeMillis() - 24 * 3600 * 1000);
            db.delete();
        } catch (Exception ex) {
            Logger.getLogger(SessionTokens.class).error("Failed to clean expired session tokens", ex);
        }
    }
}
