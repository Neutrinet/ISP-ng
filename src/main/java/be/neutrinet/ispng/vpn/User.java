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

import be.neutrinet.ispng.security.OwnedEntity;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPGetter;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author wannes
 */
@LDAPObject(requestAllAttributes = true, structuralClass = "inetOrgPerson", auxiliaryClass = {"ispngAccount", "extensibleObject"})
public class User implements OwnedEntity {

    // Currently allowed countries = benelux
    public transient final String[] ALLOWED_COUNTRIES = new String[]{"BELGIUM", "NETHERLANDS", "LUXEMBOURG"};
    @LDAPField(attribute = "uid", objectClass = "inetOrgPerson", requiredForEncode = true)
    public UUID globalId;
    @LDAPField(attribute = "mail", inRDN = true, requiredForEncode = true)
    public String email;
    @LDAPField(attribute = "gn", objectClass = "inetOrgPerson", requiredForEncode = true)
    public String name;
    @LDAPField(attribute = "sn", objectClass = "inetOrgPerson", requiredForEncode = true)
    public String lastName;
    @LDAPField(objectClass = "inetOrgPerson")
    public String street;
    @LDAPField(objectClass = "inetOrgPerson")
    public String postalCode;
    @LDAPField(attribute = "l", requiredForEncode = true)
    public String municipality;
    @LDAPField(objectClass = "ispngAccount")
    public String birthPlace;
    @LDAPField(objectClass = "ispngAccount")
    public Date birthDate;
    @LDAPField(objectClass = "ispngAccount")
    public boolean enabled;
    @LDAPField(attribute = "PKCScertificateIdentifier", objectClass = "ispngAccount")
    public String certId;
    @LDAPField(attribute = "countryName", objectClass = "extensibleObject")
    public String country;
    @LDAPField(attribute = "userPassword", requiredForEncode = true)
    private String password;
    private transient UserSettings settings;

    public User() {
        this.globalId = UUID.randomUUID();
    }

    @LDAPGetter(attribute = "cn")
    public String getCN() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        String salt = BCrypt.gensalt(10);
        this.password = BCrypt.hashpw(password, salt);
    }

    public void setRawPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public boolean validate() throws IllegalArgumentException {
        boolean validCountry = false;
        for (String c : ALLOWED_COUNTRIES)
            if (country.equals(c)) {
                validCountry = true;
                break;
            }
        if (!validCountry) throw new IllegalArgumentException("Invalid country " + country);

        Calendar cal = Calendar.getInstance();
        cal.setTime(birthDate);
        int birthYear = cal.get(Calendar.YEAR);
        cal.setTime(new Date());
        int delta = cal.get(Calendar.YEAR) - birthYear;

        if (delta > 100) {
            throw new IllegalArgumentException("You are more than a hundred years old? "
                    + "Impressive! Please contact us to complete your registration.");
        } else if (delta < 10) {
            throw new IllegalArgumentException("Younger than 12? You're well ahead of your time.");
        }

        return true;
    }

    public UserSettings settings() {
        if (settings == null) {
            settings = new UserSettings("" + globalId);
            settings.load();
        }

        return settings;
    }

    @Override
    public boolean isOwnedBy(User user) {
        if (user == null) return false;
        return user.globalId == globalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!globalId.equals(user.globalId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return globalId.hashCode();
    }
}
