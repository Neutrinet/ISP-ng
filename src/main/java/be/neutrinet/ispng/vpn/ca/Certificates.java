package be.neutrinet.ispng.vpn.ca;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Created by wannes on 04/07/14.
 */
public class Certificates {

    public static Dao<Certificate, String> dao;

    static {
        Class cls = Certificate.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);

        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }
}
