/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.admin;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Date;

/**
 *
 * @author wannes
 */
@DatabaseTable(tableName = "vpn_unlock_keys")
public class UnlockKey {

    @DatabaseField(id = true)
    public String key;
    @DatabaseField(canBeNull = false, defaultValue = "*")
    public String email;
    @DatabaseField
    public Date usedAt;
}
