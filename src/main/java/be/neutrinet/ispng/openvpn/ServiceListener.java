/*
 * ServiceListener.java
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

/**
 * @author wannes
 */
public interface ServiceListener {

    public void clientConnect(Client client);

    public void connectionEstablished(Client client);

    public void clientDisconnect(Client client);

    public void clientReAuth(Client client);

    public void addressInUse(Client client, String address, boolean primary);

    public void bytecount(Client client, long bytesIn, long bytesOut);

    public void setManagementInterface(ManagementInterface mgmt);

    public void managementConnectionEstablished();
}
