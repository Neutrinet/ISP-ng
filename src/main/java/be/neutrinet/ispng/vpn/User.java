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
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Security;
import java.util.*;

/**
 * @author wannes
 */
@LDAPObject(requestAllAttributes = true, structuralClass = "inetOrgPerson", auxiliaryClass = {"ispngAccount", "extensibleObject"})
public class User implements OwnedEntity {

    // Currently allowed countries = benelux
    public transient final String[] ALLOWED_COUNTRIES = new String[]{"BE", "NL", "LU", "FR"};
    @LDAPField(attribute = "uid", objectClass = "inetOrgPerson", requiredForEncode = true)
    public UUID id;
    @LDAPField(attribute = "mail", inRDN = true, requiredForEncode = true)
    public String email;
    @LDAPField(attribute = "givenName", objectClass = "inetOrgPerson", requiredForEncode = true)
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
        this.id = UUID.randomUUID();
    }

    @LDAPGetter(attribute = "cn")
    public String getCN() {
        return email;
    }

    public String getDN() {
        return "mail=" + email + "," + Users.usersDN();
    }

    public void setPassword(String password) {
        assert password != null;

        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] salt = new byte[4];
            new Random().nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-512", "BC");
            md.reset();
            md.update(password.getBytes());
            md.update(salt);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(md.digest());
            outputStream.write(salt);

            byte[] digest = outputStream.toByteArray();

            this.password = "{ssha512}" + Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).fatal("Failed to set password. This is a fatal error, shutting down", ex);
            System.exit(1);
        }
    }

    public String getPassword() {
        return password;
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
            settings = new UserSettings("" + id);
            settings.load();
        }

        return settings;
    }

    @Override
    public boolean isOwnedBy(UUID user) {
        return user != null && user.equals(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return id.equals(user.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
