package be.neutrinet.ispng.vpn.ip;

import be.neutrinet.ispng.VPN;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Subnet allocator, slab like
 * Known to be inefficient
 * <p>
 * Created by wannes on 11/15/14.
 */
public class IPSubnets {

    public static Dao<IPSubnet, String> dao;

    static {
        Class cls = IPSubnet.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);

            createPoolSubnets();
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }

    private static void createPoolSubnets() {
        try {
            IPv6Network pool = IPv6Network.fromString(VPN.cfg.getProperty("vpn.ipv6.pool.subnet"));
            List<IPSubnet> subnet = dao.queryForEq("subnet", pool.toString());
            if (subnet.size() != 1) {
                IPSubnet rootSubnet = new IPSubnet();
                rootSubnet.ipVersion = 6;
                rootSubnet.prefix = pool.getNetmask().asPrefixLength();
                rootSubnet.subAllocate = true;
                rootSubnet.subnet = pool.toString();
                dao.create(rootSubnet);
            }
        } catch (SQLException ex) {
            Logger.getLogger(IPSubnets.class).error("Failed to check for root subnet", ex);
        }
    }

    /**
     * @param prefix    IP prefix
     * @param ipVersion IGNORED ATM
     * @return
     */
    public static IPSubnet allocate(int prefix, boolean allowSubAllocation, int ipVersion) {
        if (prefix < 4 || prefix > 64) throw new IllegalArgumentException("Can only allocate subnet from /4 to /64");

        // check for range to alloc from
        try {
            QueryBuilder<IPSubnet, String> qb = dao.queryBuilder();
            qb.where().eq("prefix", prefix - 1).and().eq("subAllocate", true);

            for (Iterator<IPSubnet> it = qb.iterator(); ; ) {
                IPSubnet parent = null;

                if (!it.hasNext()) {
                    if (prefix != 4) {
                        parent = allocate(prefix - 1, true, 6);
                        if (parent == null) return null;
                    } else return null;
                } else parent = it.next();

                List<IPSubnet> siblings = dao.queryForEq("parentId", parent.id);

                IPv6Network ipv6Network = IPv6Network.fromString(parent.subnet);
                for (Iterator<IPv6Network> ite = ipv6Network.split(IPv6NetworkMask.fromPrefixLength(prefix)); ite.hasNext(); ) {
                    IPv6Network n = ite.next();

                    if (siblings.stream().filter(no -> no.subnet.equals(n.toString())).count() != 0) {
                        continue;
                    } else {
                        IPSubnet ns = new IPSubnet();
                        ns.ipVersion = 6;
                        ns.parentId = parent.id;
                        ns.prefix = prefix;
                        ns.subnet = n.toString();
                        ns.subAllocate = allowSubAllocation;

                        dao.create(ns);

                        return ns;
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(IPSubnets.class).error("Failed to alloc subnet", ex);
        }

        return null;
    }
}
