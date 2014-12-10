package be.neutrinet.ispng.vpn.ip;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by wannes on 11/15/14.
 */
@DatabaseTable(tableName = "subnets")
public class IPSubnet {
    @DatabaseField(generatedId = true)
    public long id;
    @DatabaseField(canBeNull = false)
    public String subnet;
    @DatabaseField
    public int prefix;
    @DatabaseField
    public boolean subAllocate;
    @DatabaseField
    public long parentId;
    @DatabaseField(defaultValue = "6")
    public int ipVersion;

    public Optional<IPSubnet> parent() {
        try {
            IPSubnet ipSubnet = IPSubnets.dao.queryForId("" + parentId);
            return Optional.of(ipSubnet);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to retrieve parent", ex);
        }

        return Optional.empty();
    }
}
