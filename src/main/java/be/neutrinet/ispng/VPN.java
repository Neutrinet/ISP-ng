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

import be.fedict.eid.applet.service.AppletServiceServlet;
import be.neutrinet.ispng.vpn.Manager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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
        /*cs = new JdbcConnectionSource("jdbc:mariadb://vpn.w-gr.net/ispng", 
         "neutrinet", "password", new MariaDBType());
         */
        cs = new JdbcConnectionSource("jdbc:sqlite:test.db");

        Users.createDummyUser();

        Manager.get().start();

        Server s = new Server();

        SslContextFactory scf = new SslContextFactory(true);
        scf.setKeyStorePath("keystore");
        scf.setKeyStorePassword("password");

        ServerConnector serverConnector = new ServerConnector(s, scf);
        serverConnector.setName("SSL");
        serverConnector.setPort(8080);
        s.addConnector(serverConnector);

        ResourceHandler rh = new ResourceHandler();
        rh.setDirectoriesListed(true);
        rh.setWelcomeFiles(new String[]{"index.html"});
        rh.setResourceBase("web/public_html/");

        ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
        sch.setContextPath("/");

        RestletServlet rs = new RestletServlet();
        sch.addServlet(new ServletHolder(rs), "/api/*");

        sch.addServlet(FlowServlet.class, "/flow/*");

        AppletServiceServlet ass = new AppletServiceServlet();
        // Set fields to query from eID cards
        ServletHolder assh = new ServletHolder(ass);
        assh.setInitParameter("IncludeAddress", "true");
        assh.setInitParameter("InludeCertificates", "true");
        sch.addServlet(assh, "/applet/*");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{rh, sch, new DefaultHandler()});
        s.setHandler(handlers);

        s.start();
    }
}
