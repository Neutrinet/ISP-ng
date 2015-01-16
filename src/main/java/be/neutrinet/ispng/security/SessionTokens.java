package be.neutrinet.ispng.security;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
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
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);

        }
    }
}
