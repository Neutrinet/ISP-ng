/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.neutrinet.ispng.vpn.admin;

import be.neutrinet.ispng.VPN;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * @author wannes
 */
public class Registrations {
    public static Dao<Registration, String> dao;

    static {
        Class cls = Registration.class;
        try {
            dao = DaoManager.createDao(VPN.cs, cls);
            TableUtils.createTableIfNotExists(VPN.cs, cls);
        } catch (SQLException ex) {
            Logger.getLogger(cls).error("Failed to create DAO", ex);
        }
    }
}
