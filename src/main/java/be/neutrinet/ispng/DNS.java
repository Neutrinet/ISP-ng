package be.neutrinet.ispng;

import be.neutrinet.ispng.dns.RequestHandler;
import be.neutrinet.ispng.dns.TCPServer;
import be.neutrinet.ispng.dns.UDPServer;
import be.neutrinet.ispng.dns.ZoneBuilder;
import be.neutrinet.ispng.util.Zookeeper;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xbill.DNS.*;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wannes on 1/24/15.
 */
public class DNS {
    public static Map<String, TSIG> TSIG = new HashMap<>();
    public static Map<Name, Zone> zones = new HashMap<>();
    public static Properties cfg;

    public static void main(String[] args) {
        try {
            Logger root = Logger.getRootLogger();
            root.setLevel(Level.INFO);
            root.addAppender(new ConsoleAppender(VPN.LAYOUT));

            cfg = new Properties();
            cfg.load(new FileInputStream("dns.properties"));

            Zookeeper.boot(cfg.getProperty("zookeeper.connectionString"));

            String TSIGname = cfg.getProperty("tsig.name").toLowerCase() + ".";
            TSIG.put(TSIGname, new TSIG(cfg.getProperty("tsig.algorithm"), TSIGname, cfg.getProperty("tsig.key")));

            ZoneBuilder zoneBuilder = new ZoneBuilder();
            zoneBuilder.boot(cfg);
            zones = zoneBuilder.rebuildZones();

            RequestHandler handler = new RequestHandler();
            UDPServer udp = new UDPServer(InetAddress.getByName("0.0.0.0"), 5252, handler);
            new Thread(udp, "DNS-UDP").start();
            TCPServer tcp = new TCPServer(InetAddress.getByName("0.0.0.0"), 5252, handler);
            new Thread(tcp, "DNS-TCP").start();

        } catch (Exception ex) {
            Logger.getLogger(DNS.class).error("Failed to start DNS server", ex);
        }
    }

    private static void buildIPv4Zone() {
        try {
            Name ipv4 = Name.fromString("181.67.80.in-addr.arpa.");

            PTRRecord ptrRecord = new PTRRecord(Name.fromString("134", ipv4), DClass.IN, 3600, Name.fromString("seriouscat.net."));
            PTRRecord vpn = new PTRRecord(Name.fromString("1", ipv4), DClass.IN, 3600, Name.fromString("vpn.neutrinet.be."));
            NSRecord ns1Record = new NSRecord(Name.fromString("@", ipv4), DClass.IN, 3600, Name.fromString("ns1.neutrinet.be."));
            NSRecord ns2Record = new NSRecord(Name.fromString("@", ipv4), DClass.IN, 3600, Name.fromString("ns2.neutrinet.be."));
            SOARecord soaRecord = new SOARecord(Name.fromString("@", ipv4), DClass.IN, 3600, Name.fromString("ns.neutrinet.be."),
                    Name.fromString("dns.neutrinet.be."), 20150125, 86400, 86400, 2419200, 86400);
            Zone zone = new Zone(ipv4, new Record[]{soaRecord, ptrRecord, ns1Record, ns2Record, vpn});

            zones.put(ipv4, zone);
        } catch (Exception ex) {
            Logger.getLogger(DNS.class).error("Failed to build v4 zone", ex);
        }
    }

    private static void buildIPv6Zone() {
        try {
            Name ipv6 = Name.fromString("1.3.1.9.0.1.0.0.2.ip6.arpa.");

            PTRRecord ptrRecord = new PTRRecord(Name.fromString("1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.2.1.0.0.0", ipv6), DClass.IN, 3600, Name.fromString("seriouscat.net."));
            PTRRecord vpn = new PTRRecord(Name.fromString("1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0", ipv6), DClass.IN, 3600, Name.fromString("vpn.neutrinet.be."));
            NSRecord ns1Record = new NSRecord(Name.fromString("@", ipv6), DClass.IN, 3600, Name.fromString("ns1.neutrinet.be."));
            NSRecord ns2Record = new NSRecord(Name.fromString("@", ipv6), DClass.IN, 3600, Name.fromString("ns2.neutrinet.be."));
            SOARecord soaRecord = new SOARecord(Name.fromString("@", ipv6), DClass.IN, 3600, Name.fromString("ns.neutrinet.be."),
                    Name.fromString("dns.neutrinet.be."), 20150125, 86400, 86400, 2419200, 86400);
            Zone zone = new Zone(ipv6, new Record[]{soaRecord, ptrRecord, ns1Record, ns2Record, vpn});

            zones.put(ipv6, zone);
        } catch (Exception ex) {
            Logger.getLogger(DNS.class).error("Failed to build v6 zone", ex);
        }
    }
}
