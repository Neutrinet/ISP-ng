/*
 * ManagementClient.java
 * Copyright (C) Apr 5, 2014 Wannes De Smet
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
package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.config.Config;
import be.neutrinet.ispng.vpn.Manager;
import com.google.common.collect.LinkedListMultimap;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wannes
 */
public class ManagementInterface implements Runnable {

    private final Watchdog watchdog = new Watchdog();
    private final ServiceListener listener;
    protected boolean echoOpenVPNCommands = false;
    protected String host;
    protected int port;
    protected String instanceId;
    private Thread thread;
    private Socket sock;
    private BufferedReader br;
    private BufferedWriter bw;
    private String line;
    private boolean run;

    public ManagementInterface(ServiceListener listener, String host, int port) {
        this.listener = listener;
        this.host = host;
        this.port = port;
        this.instanceId = host + ":" + port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void connect() throws IOException {
        listener.setManagementInterface(this);
        // dispose of possible old socket
        if (sock != null && !sock.isConnected()) sock.close();
        // check if a connection already exists
        if (sock != null && !sock.isClosed()) return;

        sock = new Socket(host, port);
    }

    public void connected() {
        listener.managementConnectionEstablished();

        Config.get().getAndWatch("debug/OpenVPN/echoCommands", "false", (String value) -> echoOpenVPNCommands = value.equals("true"));
    }

    public Watchdog getWatchdog() {
        return watchdog;
    }

    public void authorizeClient(int id, int kid) {
        String authorize = "client-auth-nt " + id + ' ' + kid + '\n';
        try {
            writeLine(authorize);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    public void authorizeClient(int id, int kid, LinkedListMultimap<String, String> options) {
        String authorize = "client-auth " + id + ' ' + kid + '\n';
        try {
            writeLine(authorize);
            for (Map.Entry<String, String> entry : options.entries()) {
                writeLine(entry.getKey() + (entry.getValue() != null ? " " + entry.getValue() : "") + '\n');
            }
            writeLine("END\n");
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    public void denyClient(int id, int kid, String reason) {
        String cmd = String.format("client-deny %s %s DENIED \"%s\"\n", id, kid, reason);
        try {
            writeLine(cmd);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    public void killClient(int id) {
        String cmd = String.format("client-kill %s \n", id);
        try {
            writeLine(cmd);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    protected Client buildClient(String[] args) throws IOException {
        boolean reachedEnd = false;
        HashMap<String, String> clientEnv = new HashMap<>();

        while (!reachedEnd) {
            line = br.readLine();

            if (line.equals(">CLIENT:ENV,END")) {
                reachedEnd = true;
                Client client = ObjectMarshall.fuzzyBuild(clientEnv, Client.class);
                client.id = Integer.parseInt(args[0]);
                if (args.length > 1) {
                    client.kid = Integer.parseInt(args[1]);
                }
                return client;
            }

            String key = line.substring(line.indexOf(',') + 1, line.indexOf('='));
            String value = line.substring(line.indexOf('=') + 1);

            clientEnv.put(key, value);
        }

        return null;
    }

    public void setBandwidthMonitoringInterval(int interval) {
        try {
            if (interval < 0) throw new IllegalArgumentException("interval cannot be lower than zero");
            writeLine("bytecount " + interval + "\n");
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    private synchronized void writeLine(String line) throws IOException {
        if (echoOpenVPNCommands)
            Logger.getLogger(getClass()).debug("Sent OpenVPN command '" + line + "'");

        System.out.print(line);
        bw.write(line);
        bw.flush();
    }

    @Override
    public void run() {
        main:
        while (run) {
            try {
                connect();

                this.br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                this.bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

                connected();

                line = br.readLine();

                commandHandling:
                while (run) {
                    if (line == null) {
                        // Abort and recover
                        break;
                    }

                    if (line.startsWith(">")) {
                        if (echoOpenVPNCommands)
                            Logger.getLogger(getClass()).debug("OpenVPN command: " + line);

                        int cmdSep = line.indexOf(',');
                        String cmd, subCmd = "";
                        String[] args;
                        if (cmdSep != -1) {
                            cmd = line.substring(1, cmdSep);
                            args = line.substring(cmdSep + 1).split(",");
                            int subCmdSep = cmd.indexOf(':');
                            if (subCmdSep != -1) {
                                subCmd = cmd.substring(subCmdSep + 1);
                                cmd = cmd.substring(0, subCmdSep);
                            }
                        } else {
                            cmdSep = line.indexOf(':');
                            cmd = line.substring(1, cmdSep);
                            args = new String[]{line.substring(cmdSep + 1)};
                        }

                        switch (cmd) {
                            case "CLIENT":
                                switch (subCmd) {
                                    case "CONNECT":
                                        Client client = buildClient(args);
                                        listener.clientConnect(client);
                                        break;
                                    case "ESTABLISHED":
                                        client = buildClient(args);
                                        listener.connectionEstablished(client);
                                        break;
                                    case "DISCONNECT":
                                        client = buildClient(args);
                                        listener.clientDisconnect(client);
                                        break;
                                    case "ADDRESS":
                                        client = new Client();
                                        client.id = Integer.parseInt(args[0]);
                                        listener.addressInUse(client, args[1], args[2].equals("1"));
                                        break;
                                    case "REAUTH":
                                        client = buildClient(args);
                                        listener.clientReAuth(client);
                                        break;
                                }
                                break;
                            case "BYTECOUNT_CLI":
                                Client client = new Client();
                                client.id = Integer.parseInt(subCmd);
                                listener.bytecount(client, Long.parseLong(args[0]), Long.parseLong(args[1]));
                                break;
                            case "INFO":
                                Logger.getLogger(getClass()).info("OpenVPN: " + line);
                                break;
                            default:
                                Logger.getLogger(getClass()).warn("Unhandled OpenVPN command " + cmd);
                                break;
                        }
                    }

                    line = br.readLine();
                }

                sock.close();
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("Management client failure", ex);
                break;
            }
        }
    }

    public final class Watchdog extends Thread {

        @Override
        public void run() {
            run = true;

            try {
                while (run) {
                    // Threads cannot be restarted, create a new instance each time
                    thread = new Thread(ManagementInterface.this, "ManagementClient-" + host + ":" + port);
                    thread.start();
                    thread.join();

                    if (run) {
                        Logger.getLogger(getClass()).warn("Recovering from management interface failure");

                        // wait one second 'til recover attempt
                        Thread.sleep(10000000);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("Recovery failure", ex);
                Manager.get().shutItDown("Could not recover from mgmt client failure");
                run = false;
            }
        }
    }
}
