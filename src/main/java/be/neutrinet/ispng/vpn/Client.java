package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Created by wannes on 11/10/14.
 */
@DatabaseTable(tableName = "ovpn_clients")
public class Client implements Serializable {

    public static Dao<Client, String> dao;
    static {
        try {
            dao = DaoManager.createDao(VPN.cs, Client.class);
            TableUtils.createTableIfNotExists(VPN.cs, Client.class);
        } catch (SQLException ex) {
            Logger.getLogger(Client.class).error("Failed to create DAO", ex);
        }
    }

    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField
    public String platform;
    @DatabaseField(canBeNull = false)
    public String commonName;
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public User user;

    public static Optional<Client> match(be.neutrinet.ispng.openvpn.Client vpnClient) {
        try {
            List<User> users = Users.dao.queryForEq("email", vpnClient.username);

            assert users.size() == 1;

            User user = users.get(0);
            HashMap<String, Object> query = new HashMap<>();
            query.put("user_id", user.id);
            query.put("commonName", vpnClient.commonName);
            List<Client> clients = dao.queryForFieldValues(query);

            if (clients.size() > 1) {
                Logger.getLogger(Client.class).error("Multiple client definitions, user: " + vpnClient.username +
                ", commonName: " + vpnClient.commonName);
                return Optional.empty();
            }

            if (clients.isEmpty()) return Optional.empty();
            else return Optional.of(clients.get(0));
        } catch (Exception ex) {
            Logger.getLogger(Client.class).warn("Failed to match VPN client", ex);
        }

        return Optional.empty();
    }

    public static Client create(be.neutrinet.ispng.openvpn.Client client) {
        Client c = new Client();
        c.commonName = client.commonName;
        c.platform = client.platform;
        try {
            List<User> users = Users.dao.queryForEq("email", client.username);

            assert users.size() == 1;

            User user = users.get(0);
            c.user = user;

            dao.createIfNotExists(c);
        } catch (Exception ex) {
            Logger.getLogger(Client.class).error("Failed to create VPN client", ex);
        }

        return c;
    }
}
