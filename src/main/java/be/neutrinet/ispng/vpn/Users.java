/*
 * Users.java
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
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author wannes
 */
public class Users {

    public static Dao<User, String> dao;
    public static User NOBODY;

    public final static User authenticate(String email, String password) {
        try {
            List<User> users = dao.queryForEq("email", email);
            if (users.isEmpty()) {
                return null;
            }
            if (users.size() > 1) {
                throw new IllegalStateException("User " + email + " has multiple entries!");
            }
            User user = users.get(0);
            if (user.enabled && user.validatePassword(password)) {
                return user;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Users.class).error("Failed to authenticate", ex);
        }

        return null;
    }

    static {
        Class cls = User.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);
        } catch (SQLException ex) {
            org.apache.log4j.Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
        
        NOBODY = new User();
        NOBODY.id = -1;
    }
}
