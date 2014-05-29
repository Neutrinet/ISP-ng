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
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author double-u
 */
public class Users {

    public static Dao<User, String> dao;

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

    public final static void createDummyUser() throws SQLException {
        User dummy = new User();
        dummy.email = "ackbar@neutrinet.be";
        dummy.setPassword("it'satrapIV");
        dummy.name = "Admiral J.P.";
        dummy.lastName = "Ackbar";
        dummy.enabled = true;
        dummy.birthDate = new Date();
        dummy.birthPlace = "Space";
        dao.createIfNotExists(dummy);
    }

    static {
        Class cls = User.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);
        } catch (SQLException ex) {
            org.apache.log4j.Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }
}
