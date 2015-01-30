package be.neutrinet.ispng.dns;

import be.neutrinet.ispng.util.Zookeeper;
import net.killa.kept.KeptList;
import net.killa.kept.KeptMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by wannes on 1/26/15.
 */
public class ZoneBuilder {

    private final static String PREFIX = "/ispng/dns/";
    private KeptMap zones;
    private Map<String, KeptList<DNSRecord>> records = new HashMap<>();
    // TODO persist zone serial number suffix
    private Map<String, Integer> suffixes = new HashMap<>();
    private final static SimpleDateFormat SERIAL_NUMBER_FORMAT = new SimpleDateFormat("YYYYMMDD");
    public static Name SAME_AS_ROOT;
    private int ttl;
    private List<String> nameServers = new ArrayList<>();
    private Properties cfg;

    static {
        try {
            SAME_AS_ROOT = Name.fromString("@");
        } catch (Exception ex) {
            Logger.getLogger(ZoneBuilder.class).error("Something went terribly wrong", ex);
        }
    }

    public void boot(Properties cfg) {
        this.cfg = cfg;
        this.ttl = Integer.parseInt(cfg.getProperty("zone.all.record.TTL"));
        for (String part : cfg.getProperty("zone.all.nameservers").split(";")) {
            this.nameServers.add(part.trim());
        }

        boot();
    }

    public void boot() {
        CuratorFramework cf = Zookeeper.get();

        try {
            String dir = "/ispng/dns";
            Stat stat = cf.checkExists().forPath(dir);
            if (stat == null)
                cf.create().creatingParentsIfNeeded().forPath(dir, null);

            zones = new KeptMap(cf.getZookeeperClient().getZooKeeper(),
                    PREFIX + "zoneMap", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to boot DNS zones", ex);
        }
    }

    public void renderZonesToZK() {
        try {
            Map<String, List<DNSRecord>> zs = DNSRecords.buildZones();
            zones.clear();
            for (Map.Entry<String, List<DNSRecord>> entry : zs.entrySet()) {
                String label = entry.getKey();
                zones.put(label, DNSRecords.labelToZone.get(entry.getKey()));

                KeptList<DNSRecord> kl = new KeptList<>(DNSRecord.class, Zookeeper.getZK(),
                        PREFIX + "zones/" + label, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                kl.clear();
                kl.addAll(entry.getValue());

                records.put(label, kl);
            }

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to rebuild zone info from DB", ex);
        }
    }

    public Map<Name, Zone> rebuildZones() {
        HashMap<Name, Zone> zones = new HashMap<>();
        try {
            for (Map.Entry<String, String> entry : this.zones.entrySet()) {
                KeptList<DNSRecord> kl = new KeptList<>(DNSRecord.class, Zookeeper.getZK(),
                        PREFIX + "zones/" + entry.getKey(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

                zones.put(Name.fromString(entry.getKey()), buildZone(entry.getValue(), kl));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to rebuild zones", ex);
        }

        return zones;
    }

    public Zone buildZone(String zoneRoot, List<DNSRecord> DNSRecords) {
        try {
            ArrayList<Record> dnsRecords = new ArrayList<>();
            Name root = Name.fromString(zoneRoot);
            int suffix = suffixes.getOrDefault(zoneRoot, 1);
            int serial = Integer.parseInt(SERIAL_NUMBER_FORMAT.format(new Date()) + String.format("%1$" + 2 + "s", suffix));
            suffixes.put(zoneRoot, suffix + 1);

            // SOA record
            dnsRecords.add(buildSOARecord(Name.concatenate(SAME_AS_ROOT, root), serial));

            // NS records
            for (String ns : nameServers) {
                dnsRecords.add(new NSRecord(Name.concatenate(SAME_AS_ROOT, root), DClass.IN, ttl, Name.fromString(ns)));
            }

            // * records
            for (DNSRecord r : DNSRecords)
                dnsRecords.add(buildDNSRecord(root, r));

            return new Zone(root, dnsRecords.toArray(new Record[]{}));
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to build zone " + zoneRoot, ex);
        }

        return null;
    }

    private SOARecord buildSOARecord(Name name, int serial) throws Exception {
        return new SOARecord(
                name,
                DClass.IN,
                ttl,
                Name.fromString(cfg.getProperty("zone.all.soa.nameserver")),
                Name.fromString(cfg.getProperty("zone.all.soa.admin")),
                serial,
                Integer.parseInt(cfg.getProperty("zone.all.soa.refresh")),
                Integer.parseInt(cfg.getProperty("zone.all.soa.retry")),
                Integer.parseInt(cfg.getProperty("zone.all.soa.expiry")),
                Integer.parseInt(cfg.getProperty("zone.all.soa.nxdomainTTL"))
        );
    }

    private Record buildDNSRecord(Name root, DNSRecord record) throws Exception {
        switch (record.getType()) {
            case DNSRecord.PTR:
                return new PTRRecord(Name.fromString(record.name, root),
                        DClass.IN, ttl, Name.fromString(record.target));
            case DNSRecord.NS:
                return new NSRecord(Name.fromString(record.name, root),
                        DClass.IN, ttl, Name.fromString(record.target));
            case DNSRecord.AAAA:
                return new AAAARecord(Name.fromString(record.name, root),
                        DClass.IN, ttl, InetAddress.getByName(record.target));
            case DNSRecord.A:
                return new ARecord(Name.fromString(record.name, root),
                        DClass.IN, ttl, InetAddress.getByName(record.target));
            default:
                throw new IllegalArgumentException("Invalid DNS record type");
        }
    }

    public void addOrUpdate(DNSRecord r, String zoneLabel) {

    }
}
