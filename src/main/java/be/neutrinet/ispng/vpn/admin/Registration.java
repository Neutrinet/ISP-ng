/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.admin;

import be.neutrinet.ispng.DateUtil;
import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.*;
import be.neutrinet.ispng.vpn.api.VPNClientCertificate;
import be.neutrinet.ispng.vpn.ca.Certificate;
import be.neutrinet.ispng.vpn.ca.Certificates;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 *
 * @author wannes
 */
@DatabaseTable(tableName = "registrations")
public class Registration {

    private static final Map<UUID, Registration> activeRegistrations = new HashMap<>();
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public User user;
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public Client client;
    @DatabaseField(canBeNull = false)
    public Date timeInitiated;
    @DatabaseField
    public int ipv4Id;
    @DatabaseField
    public int ipv6Id;
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    public UnlockKey unlockKey;
    @DatabaseField
    public Date completed;
    @DatabaseField(id = true, canBeNull = false)
    private UUID id;

    private Registration() {

    }

    public Registration(UUID id) {
        this.id = id;
    }

    public static Map<UUID, Registration> getActiveRegistrations() {
        return activeRegistrations;
    }

    public UUID getId() {
        return id;
    }

    public void createInitialClient() {
        if (this.user == null) throw new IllegalStateException("No user is coupled");

        try {
            this.client = new Client();
            this.client.commonName = "!!TEMPORARY_CN!!";
            this.client.user = this.user;

            Clients.dao.create(client);
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to create initial client", ex);
        }
    }

    /**
     * Commits and finalizes registration
     */
    public void commit() {
        try {
            TransactionManager.callInTransaction(VPN.cs, () -> {
                if (ipv4Id != 0) {
                    IPAddress ip4 = IPAddresses.dao.queryForId("" + this.ipv4Id);
                    ip4.expiry = DateUtil.convert(LocalDate.now().plusDays(365L));
                    ip4.leasedAt = new Date();
                    ip4.client = this.client;
                    IPAddresses.dao.update(ip4);
                }

                assert unlockKey.usedAt == null;
                unlockKey.usedAt = this.timeInitiated;
                UnlockKeys.dao.update(unlockKey);

                this.completed = new Date();
                this.client.user.enabled = true;
                Users.dao.update(this.client.user);

                // Check if user has certificates that need to be signed
                List<Certificate> certs = Certificates.dao.queryForEq("client_id", client.id);
                for (Certificate cert : certs) {
                    VPNClientCertificate.sign(cert);
                }
                
                Registrations.dao.update(this);
                VPN.generator.sendRegistrationConfirmation(this);
                return true;
            });
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Registration failed", ex);
        }
    }
}
