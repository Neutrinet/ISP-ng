package be.neutrinet.ispng.dns;

import be.neutrinet.ispng.security.OwnedEntity;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.User;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by wannes on 1/27/15.
 */
@DatabaseTable
public class DNSRecord implements Serializable, OwnedEntity {

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(canBeNull = false)
    public String name;
    @DatabaseField(canBeNull = false)
    public String target;
    @DatabaseField(defaultValue = "3600")
    public int ttl;
    @DatabaseField(canBeNull = false)
    private String type;
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public transient Client client;

    public final static String A = "A";
    public final static String AAAA = "AAAA";
    public final static String PTR = "PTR";
    public final static String NS = "NS";
    public final static String[] ALLOWED_TYPES = new String[]{A, AAAA, PTR, NS};

    private DNSRecord() {

    }

    public DNSRecord(String name, String target, int ttl, String type) {
        this.name = name;
        this.target = target;
        this.ttl = ttl;
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
    public boolean isOwnedBy(User user) {
        return this.client.user.equals(user);
    }
}
