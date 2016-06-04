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

import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.external.LDAP;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import com.unboundid.ldap.sdk.persist.ObjectSearchListener;
import com.unboundid.ldap.sdk.persist.PersistedObjects;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author wannes
 */
public class Users {

    public static LDAPPersister<User> persister;
    public static User NOBODY;

    static {
        Class cls = User.class;
        try {
            persister = LDAPPersister.getInstance(cls);
        } catch (LDAPPersistException ex) {
            org.apache.log4j.Logger.getLogger(cls).error("Failed to create LDAP persister", ex);
        }

        NOBODY = new User();
        NOBODY.id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public static User authenticate(String email, String password) {

        assert email != null;
        assert password != null;

        if (LDAP.get().auth("mail=" + LDAP.escapeDN(email) + "," + usersDN(), password)) {
            return get(email);
        }

        return null;
    }

    public static void add(User user) throws LDAPPersistException {
        try {
            persister.add(user, LDAP.connection(), usersDN());
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to add user", ex);
            throw ex;
        }
    }

    public static List<User> query(String field, Object value) {
        ArrayList<User> users = new ArrayList<>();
        try {
            PersistedObjects<User> objects = persister.search(LDAP.connection(),
                    usersDN(),
                    SearchScope.SUB,
                    DereferencePolicy.ALWAYS,
                    Integer.MAX_VALUE,
                    0,
                    Filter.create("(" + LDAP.findAttributeName(User.class, field) + "=" + value + ")"));

            User user = objects.next();
            while (user != null) {
                users.add(user);
                user = objects.next();
            }

            objects.close();

        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to query LDAP for user(s)", ex);
        }

        return users;
    }

    public static User get(String email) {
        try {
            return persister.get("mail=" + LDAP.escapeDN(email) + "," + usersDN(), LDAP.connection());
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to get user", ex);
        }

        return null;
    }

    public static User queryForId(String id) {
        List<User> users = query("uid", id);
        assert users.size() <= 1;
        if (users.size() == 1) return users.get(0);
        else return null;
    }

    public static User queryForId(UUID id) {
        assert id != null;
        return queryForId(id.toString());
    }

    public static List<User> queryForAll() {
        ArrayList<User> users = new ArrayList<>();
        try {
            persister.getAll(LDAP.connection(), usersDN(), new ObjectSearchListener<User>() {
                @Override
                public void objectReturned(User user) {
                    users.add(user);
                }

                @Override
                public void unparsableEntryReturned(SearchResultEntry searchResultEntry, LDAPPersistException e) {

                }

                @Override
                public void searchReferenceReturned(SearchResultReference searchResultReference) {

                }
            });
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to list all users", ex);
        }

        return users;
    }

    public static User update(User user) {
        try {
            persister.modify(user, LDAP.connection(), user.getDN(), true);
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to update user " + user.email, ex);
        }

        return user;
    }

    public static User delete(User user) {
        try {
            persister.delete(user, LDAP.connection());
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to delete user " + user.email, ex);
        }

        return user;
    }

    public static boolean isAdmin(UUID user) {
        try {
            String filter = "(&(member=" + queryForId(user).getDN() + ")(objectClass=groupOfNames))";
            SearchResult searchResult = LDAP.connection().search(
                    "dc=neutrinet,dc=be",
                    SearchScope.SUB,
                    DereferencePolicy.ALWAYS,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    Filter.create(filter));

            for (SearchResultEntry sre : searchResult.getSearchEntries()) {
                if (sre.getAttribute("cn").getValue().equals("Administrators"))
                    return true;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to get group memberships for user " + user, ex);
        }

        return false;
    }

    public static boolean isRelatedService(UUID user) {
        try {
            String filter = "(&(member=" + queryForId(user).getDN() + ")(objectClass=groupOfNames))";
            SearchResult searchResult = LDAP.connection().search(
                    "dc=neutrinet,dc=be",
                    SearchScope.SUB,
                    DereferencePolicy.ALWAYS,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    Filter.create(filter));

            for (SearchResultEntry sre : searchResult.getSearchEntries()) {
                if (sre.getAttribute("cn").getValue().equals("ServiceUsers"))
                    return true;
            }
        } catch (LDAPException ex) {
            Logger.getLogger(Users.class).error("Failed to get group memberships for user " + user, ex);
        }

        return false;
    }

    protected static String usersDN() {
        Optional<String> dn = Config.get().getValue("ldap/users/dn");
        if (!dn.isPresent()) {
            throw new IllegalArgumentException("No LDAP users DN set");
        } else return dn.get();
    }

}
