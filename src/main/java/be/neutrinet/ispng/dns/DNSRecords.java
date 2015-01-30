package be.neutrinet.ispng.dns;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by wannes on 1/27/15.
 */
public class DNSRecords {

    public static Class cls = DNSRecord.class;
    public static String ZONE_PROPERTY_PREFIX = "dns.zone.";
    public static Map<String, String> labelToZone = new HashMap<>();

    public static Optional<Dao<DNSRecord, String>> getDaoForZone(String label) {
        try {
            // Each zone gets a different table
            DatabaseTableConfig<DNSRecord> dtc = new DatabaseTableConfig<>(DNSRecord.class, "zone_" + label, null);
            Dao<DNSRecord, String> dao = DaoManager.createDao(VPN.cs, dtc);
            TableUtils.createTableIfNotExists(VPN.cs, dtc);
            return Optional.of(dao);
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }

        return Optional.empty();
    }

    public static Map<String, List<DNSRecord>> buildZones() {

        HashMap<String, List<DNSRecord>> zones = new HashMap<>();

        VPN.cfg.stringPropertyNames().stream()
                .filter(prop -> prop.startsWith(ZONE_PROPERTY_PREFIX))
                .forEach(prop -> {
                            String label = prop.substring(ZONE_PROPERTY_PREFIX.length());
                            String zoneRoot = VPN.cfg.getProperty(prop);

                            getDaoForZone(label).ifPresent((dao) -> {
                                try {
                                    List<DNSRecord> DNSRecords = dao.queryForAll();
                                    zones.put(label, DNSRecords);
                                } catch (SQLException ex) {
                                    Logger.getLogger(cls).error("Failed to create DAO", ex);
                                }
                            });

                            labelToZone.put(label, zoneRoot);
                        }
                );

        return zones;
    }

    public static Map<String, String> getZoneMapping() {
        if (labelToZone.isEmpty()) buildZones();

        return labelToZone;
    }
}
