package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Created by wannes on 10/18/14.
 */
public class Clients {
    public final static Client NONE;
    public static Dao<Client, String> dao;

    static {
        try {
            dao = DaoManager.createDao(VPN.cs, Client.class);
            TableUtils.createTableIfNotExists(VPN.cs, Client.class);
        } catch (SQLException ex) {
            Logger.getLogger(Client.class).error("Failed to create DAO", ex);
        }

        NONE = new Client();
        NONE.id = -1;
    }
}
