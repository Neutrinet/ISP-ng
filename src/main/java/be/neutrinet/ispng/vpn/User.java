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
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author wannes
 */
@DatabaseTable(tableName = "users")
public class User implements OwnedEntity {

    // Currently allowed countries = benelux
    public transient final String[] ALLOWED_COUNTRIES = new String[]{"BELGIUM", "NETHERLANDS", "LUXEMBOURG"};
    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(canBeNull = false, index = true, unique = true)
    public String email;
    @DatabaseField(canBeNull = false)
    public String name;
    @DatabaseField(canBeNull = false)
    public String lastName;
    @DatabaseField
    public String street;
    @DatabaseField
    public String postalCode;
    @DatabaseField
    public String municipality;
    @DatabaseField(canBeNull = false)
    public String birthPlace;
    @DatabaseField(canBeNull = false)
    public Date birthDate;
    @DatabaseField
    public boolean enabled;
    @DatabaseField
    public String certId;
    @DatabaseField
    public String country;
    @DatabaseField(canBeNull = false)
    private String password;
    private transient UserSettings settings;

    public boolean validatePassword(String password) {
        return BCrypt.checkpw(password, this.password);
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
            settings = new UserSettings("" + id);
            settings.load();
        }

        return settings;
    }

    @Override
    public boolean isOwnedBy(User user) {
        if (user == null) return false;
        return user.id == id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (id != user.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
