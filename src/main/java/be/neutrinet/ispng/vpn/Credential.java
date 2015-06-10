/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.neutrinet.ispng.vpn;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * @author wannes
 */
@DatabaseTable(tableName = "user_credentials")
public class Credential {

    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(canBeNull = false, foreignColumnName = "id")
    public User user;

    @DatabaseField(canBeNull = false, defaultValue = "GLOBAL")
    public String realm;

    @DatabaseField(canBeNull = false)
    public String type;

    @DatabaseField(canBeNull = false, defaultValue = "GRANT")
    public Modifier modifier;

    public enum Modifier {
        GRANT, DENY, INACTIVE
    }
}
