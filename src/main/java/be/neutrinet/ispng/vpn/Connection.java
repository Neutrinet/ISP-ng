/*
 * Connection.java
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
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author double-u
 */
@DatabaseTable(tableName = "ovpn_connections")
public class Connection {

    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(canBeNull = false)
    public int clientId;
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    public User user;
    @ForeignCollectionField(foreignFieldName = "connection")
    public Collection<IPAddress> addresses;
    @DatabaseField(canBeNull = false)
    public Date created;
    @DatabaseField
    public boolean active;
    @DatabaseField
    public Date closed;

    public Connection() {
    }

    public Connection(int clientId, User user) {
        this.clientId = clientId;
        this.user = user;
        this.active = true;
        this.created = new Date();
        this.addresses = new ArrayList<>();
    }

}
