package be.neutrinet.ispng.vpn.ip;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.Client;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by wannes on 11/15/14.
 */
public class SubnetLeases {

    public static Dao<SubnetLease, String> dao;

    static {
        Class cls = SubnetLease.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }

    public static Optional<SubnetLease> allocateDefault(Client client) {
        if (client == null) throw new IllegalArgumentException("Client is null");

        try {
            int defaultAllocPrefix = Integer.parseInt(VPN.cfg.getProperty("vpn.ipv6.pool.defaultAllocPrefix"));
            SubnetLease lease = new SubnetLease();
            lease.active = true;
            lease.client = client;
            lease.subnet = IPSubnets.allocate(defaultAllocPrefix, false, 6);
            SubnetLeases.dao.create(lease);

            return Optional.of(lease);
        } catch (SQLException ex) {
            Logger.getLogger(SubnetLeases.class).error("Failed to allocate default lease", ex);
        }

        return Optional.empty();
    }
}
