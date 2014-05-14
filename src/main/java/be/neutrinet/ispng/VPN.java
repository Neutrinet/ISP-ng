/*
 * VPN.java
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
package be.neutrinet.ispng;

import be.neutrinet.ispng.vpn.API;
import be.neutrinet.ispng.vpn.Manager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.restlet.Server;
import org.restlet.data.Protocol;

/**
 *
 * @author double-u
 */
public class VPN {

    public static final String CONSOLE_LOGPATTERN = "%d{HH:mm:ss,SSS} | %-5p | %t | %c{1.} %m%n";
    public static ConnectionSource cs;

    public static void main(String[] args) throws SQLException, Exception {
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.INFO);
        root.addAppender(new ConsoleAppender(new EnhancedPatternLayout(CONSOLE_LOGPATTERN)));

        //?useSSL=true&trustServerCertificate=true
        cs = new JdbcConnectionSource("jdbc:mariadb://vpn.w-gr.net/ispng", 
                "neutrinet", "password", new MariaDBType());
        
        Users.createDummyUser();

        Manager.get().start();
        
        Server s = new Server(Protocol.HTTP, 8080, API.class);
        s.start();
    }
}
