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

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.Client;
import be.neutrinet.ispng.vpn.Manager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author double-u
 */
public class ManagementInterface implements Runnable {

    private Socket sock;
    private final Thread thread;
    private BufferedReader br;
    private BufferedWriter bw;
    private String line;
    private boolean run;
    private final ServiceListener listener;

    public ManagementInterface(ServiceListener listener) {
        thread = new Thread(this, "ManagementClient");
        this.listener = listener;
    }

    public void connect() throws IOException {
        sock = new Socket(VPN.cfg.getProperty("openvpn.host"),
                Integer.parseInt(VPN.cfg.getProperty("openvpn.port")));
    }

    public void recover() {
        try {
            run = false;
            if (sock == null || !sock.isConnected()) {
                connect();
                thread.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Recovery failure", ex);
            Manager.get().shutItDown("Could not recover from mgmt client failure");
        }
    }

    public void authorizeClient(int id, int kid) {
        String authorize = "client-auth-nt " + id + ' ' + kid + '\n';
        try {
            writeLine(authorize);
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to write command", ex);
        }
    }

    public void authorizeClient(int id, int kid, LinkedHashMap<String, String> options) {
        String authorize = "client-auth " + id + ' ' + kid + '\n';
        try {
            writeLine(authorize);
            for (Map.Entry<String, String> entry : options.entrySet()) {
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

    private synchronized void writeLine(String line) throws IOException {
        System.out.print(line);
        bw.write(line);
        bw.flush();
    }

    @Override
    public void run() {
        run = true;

        while (run) {
            try {
                this.br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                this.bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                line = br.readLine();

                while (run && line != null) {
                    if (line.startsWith(">")) {
                        int cmdSep = line.indexOf(',');
                        String cmd;
                        String[] args;
                        if (cmdSep != -1) {
                            cmd = line.substring(1, cmdSep);
                            args = line.substring(cmdSep + 1).split(",");
                        } else {
                            cmdSep = line.indexOf(':');
                            cmd = line.substring(1, cmdSep);
                            args = new String[]{line.substring(cmdSep + 1)};
                        }

                        switch (cmd) {
                            case "CLIENT:CONNECT":
                                Client client = buildClient(args);
                                listener.clientConnect(client);
                                break;
                            case "CLIENT:ESTABLISHED":
                                client = buildClient(args);
                                listener.connectionEstablished(client);
                                break;
                            case "CLIENT:DISCONNECT":
                                client = buildClient(args);
                                listener.clientDisconnect(client);
                                break;
                            case "CLIENT:ADDRESS":
                                client = new Client();
                                client.id = Integer.parseInt(args[0]);
                                listener.addressInUse(client, args[1], args[2].equals("1"));
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

            } catch (Exception ex) {
                Logger.getLogger(getClass()).error("Management client failure", ex);
                break;
            }
        }

        if (run) {
            // Something bad happened, we should still be running
            recover();
        }
    }
}
