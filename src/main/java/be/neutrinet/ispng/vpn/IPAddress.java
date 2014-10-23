/*
 * IPAddressAssignment.java
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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 *
 * @author wannes
 */
@DatabaseTable(tableName = "address_pool")
public class IPAddress {

    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true)
    public Connection connection;
    @DatabaseField(defaultValue = "-1", foreign = true, foreignAutoRefresh = true)
    public Client client;
    @DatabaseField(canBeNull = false, unique = true)
    public String address;
    @DatabaseField(defaultValue = "4")
    public int ipVersion;
    @DatabaseField
    public boolean enabled;
    @DatabaseField
    public Date leasedAt;
    @DatabaseField
    public Date expiry;
    @DatabaseField
    public int netmask;
}
