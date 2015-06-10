package be.neutrinet.ispng.dns;

import be.neutrinet.ispng.security.OwnedEntity;
import be.neutrinet.ispng.vpn.Client;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by wannes on 1/27/15.
 */
@DatabaseTable
public class DNSRecord implements Serializable, OwnedEntity {

    public final static String A = "A";
    public final static String AAAA = "AAAA";
    public final static String PTR = "PTR";
    public final static String NS = "NS";
    public final static String[] ALLOWED_TYPES = new String[]{A, AAAA, PTR, NS};
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public transient Client client;
    @DatabaseField
    public long lastModified;
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(canBeNull = false)
    private String name;
    @DatabaseField(canBeNull = false)
    private String target;
    @DatabaseField(defaultValue = "3600")
    private int ttl;
    @DatabaseField(canBeNull = false)
    private String type;

    private DNSRecord() {

    }

    public DNSRecord(String name, String target, int ttl, String type) {
        this.name = name;
        this.target = target;
        this.ttl = ttl;
        this.lastModified = System.currentTimeMillis();
        setType(type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        for (String t : ALLOWED_TYPES) {
            if (type.equalsIgnoreCase(t)) {
                this.type = t;
                break;
            }
        }

        if (this.type == null) throw new IllegalArgumentException("Invalid DNS record type");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastModified = System.currentTimeMillis();
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
        this.lastModified = System.currentTimeMillis();
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
        this.lastModified = System.currentTimeMillis();
    }

    public boolean validate() {
        boolean validType = false;
        for (String t : ALLOWED_TYPES) {
            if (type.equalsIgnoreCase(t)) {
                validType = true;
                break;
            }
        }

        return validType && client != null;
    }

    @Override
    public boolean isOwnedBy(UUID user) {
        return this.client.userId.equals(user);
    }
}
