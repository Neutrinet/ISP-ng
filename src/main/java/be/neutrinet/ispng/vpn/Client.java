/*
 * Client.java
 * Copyright (C) Apr 6, 2014 Wannes De Smet
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.openvpn.Alias;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import java.io.Serializable;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 *
 * @author double-u
 */
@DatabaseTable(tableName = "ovpn_clients")
public class Client implements Serializable {

    @DatabaseField(id = true)
    public int id;
    @DatabaseField
    public int kid;
    @Alias(key = "n_clients")
    public int numberOfClients;
    @Alias(key = "IV_VER")
    @DatabaseField(canBeNull = false)
    public String version;
    @Alias(key = "IV_PLAT")
    @DatabaseField
    public String platform;
    @DatabaseField
    public int untrustedPort;
    @DatabaseField(canBeNull = false)
    public String untrustedIP;
    @DatabaseField(canBeNull = false)
    public String commonName;
    public String password;
    @DatabaseField(canBeNull = false)
    public String username;
    public int remotePort;
    public int localPort;
    public String protocol;
    @DatabaseField
    public int daemonPid;
    @DatabaseField
    public long daemonStartTime;
    public boolean daemonLogRedirect;
    public boolean daemon;
    public boolean verb;
    @DatabaseField
    public String config;
    @DatabaseField
    public int tunMTU;
    @DatabaseField
    public int linkMTU;
    @DatabaseField
    public String device;
    public String deviceType;
    @DatabaseField
    public boolean redirectGateway;
    public String scriptContext;
    public TLSCertificate[] certs;

    public static Dao<Client, String> dao;

    static {
        try {
            dao = DaoManager.createDao(VPN.cs, Client.class);
            TableUtils.createTableIfNotExists(VPN.cs, Client.class);
        } catch (SQLException ex) {
            Logger.getLogger(Client.class).error("Failed to create DAO", ex);
        }
    }
}
