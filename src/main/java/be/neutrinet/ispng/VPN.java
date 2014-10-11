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
import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.mail.Generator;
import be.neutrinet.ispng.monitoring.Agent;
import be.neutrinet.ispng.vpn.Manager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.log4j.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.FileInputStream;
import java.util.Optional;
import java.util.Properties;

/**
 *
 * @author double-u
 */
public class VPN implements Daemon {

    public static final String CONSOLE_LOGPATTERN = "%d{HH:mm:ss,SSS} | %-5p | %t | %c{1.} %m%n";
    public static final EnhancedPatternLayout LAYOUT = new EnhancedPatternLayout(CONSOLE_LOGPATTERN);
    public static ConnectionSource cs;
    public static Properties cfg;
    public static Generator generator;
    public static Agent monitoringAgent;
    public Server server;

    public static void main(String[] args) throws Exception {
        VPN vpn = new VPN();
        vpn.init(null);
        vpn.start();
        vpn.server.join();
        vpn.stop();
        vpn.destroy();
    }

    @Override
    public void init(DaemonContext dc) throws Exception {
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.INFO);
        root.addAppender(new ConsoleAppender(LAYOUT));

        cfg = new Properties();
        cfg.load(new FileInputStream("config.properties"));

        root.addAppender(new DailyRollingFileAppender(LAYOUT, cfg.getProperty("log.file", "ispng.log"), "'.'yyyy-MM-dd"));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.getLogger(getClass()).error("Unhandled exception", e);
            }
        });

        Config.get().boot();
        generator = new Generator();

        Config.get().getAndWatch("log/level", "INFO", level -> root.setLevel(Level.toLevel(level)));

        Optional<String> dbUser = Config.get("db/user");
        if (!dbUser.isPresent()) {
            cs = new JdbcConnectionSource(Config.get("db/uri").get());
        } else {
            if (cfg.get("db.uri").toString().contains("mariadb")) {
                cs = new JdbcConnectionSource(cfg.getProperty("db.uri"),
                        cfg.getProperty("db.user"),
                        cfg.getProperty("db.password"),
                        new MariaDBType()
                );
            } else if (cfg.get("db.uri").toString().contains("mysql")) {
                cs = new JdbcConnectionSource(cfg.getProperty("db.uri"),
                        cfg.getProperty("db.user"),
                        cfg.getProperty("db.password"),
                        new MySQLDBType());
            } else {
                cs = new JdbcConnectionSource(cfg.getProperty("db.uri"),
                        cfg.getProperty("db.user"),
                        cfg.getProperty("db.password")
                );
            }
        }
    }

    @Override
    public void start() throws Exception {
        Manager.get().start();

        monitoringAgent = new Agent();

        server = new Server();

        SslContextFactory scf = new SslContextFactory(true);
        scf.setKeyStorePath(cfg.getProperty("jetty.keyStore"));
        scf.setKeyStorePassword(cfg.getProperty("jetty.keyStorePassword"));

        ServerConnector sslConnector = new ServerConnector(server, scf);
        sslConnector.setHost(cfg.getProperty("jetty.hostname"));
        sslConnector.setPort(Integer.parseInt(cfg.getProperty("jetty.port")));

        server.addConnector(sslConnector);

        ResourceHandler rh = new ResourceHandler();
        rh.setDirectoriesListed(true);
        rh.setWelcomeFiles(new String[]{"index.html"});
        rh.setResourceBase("web/registration/");

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
        server.setHandler(handlers);

        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
        Manager.get().shutItDown("Asked to stop");
    }

    @Override
    public void destroy() {
        try {
            stop();
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Fatal error", ex);
        }
    }
}
