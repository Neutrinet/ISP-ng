/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * @author wannes
 */
public class Credentials {

    public static Dao<Credential, String> dao;

    static {
        Class cls = Credential.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);

        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }
}
