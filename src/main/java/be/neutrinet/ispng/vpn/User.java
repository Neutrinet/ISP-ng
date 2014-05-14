/*
 * User.java
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
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author double-u
 */
@DatabaseTable(tableName = "users")
public class User {

    @DatabaseField(id = true)
    public int id;
    @DatabaseField(canBeNull = false, index = true, unique = true)
    public String email;
    @DatabaseField
    public String name;
    @DatabaseField
    public String lastName;
    @DatabaseField(canBeNull = false)
    public String password;
    @DatabaseField
    public boolean enabled;

    public boolean validatePassword(String password) {
        return BCrypt.checkpw(password, this.password);
    }

}
