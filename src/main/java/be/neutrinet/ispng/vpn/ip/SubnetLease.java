package be.neutrinet.ispng.vpn.ip;

import be.neutrinet.ispng.vpn.Client;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by wannes on 11/15/14.
 */
@DatabaseTable(tableName = "subnetLease")
public class SubnetLease {

    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public IPSubnet subnet;
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    @JsonBackReference
    public Client client;
    @DatabaseField
    public boolean active;
}
