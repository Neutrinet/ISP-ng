/*
 * Client.java
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
package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.vpn.TLSCertificate;

import java.io.Serializable;

/**
 * @author double-u
 */
public class Client implements Serializable {

    public int id;
    public int kid;
    @Alias(key = "n_clients")
    public int numberOfClients;
    @Alias(key = "IV_VER")
    public String version;
    @Alias(key = "IV_PLAT")
    public String platform;
    public int untrustedPort;
    public String untrustedIP;
    public String commonName;
    public String password;
    public String username;
    public int remotePort;
    public int localPort;
    public String protocol;
    public int daemonPid;
    public long daemonStartTime;
    public boolean daemonLogRedirect;
    public boolean daemon;
    public boolean verb;
    public String config;
    public int tunMTU;
    public int linkMTU;
    public String device;
    public String deviceType;
    public boolean redirectGateway;
    public String scriptContext;
    public TLSCertificate[] certs;


}
