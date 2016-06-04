/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.Registrations;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * @author wannes
 */
public class FlowServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String[] parts = req.getRequestURI().split("/");
        String flow = parts[2];
        UUID id = UUID.fromString(parts[3]);

        switch (flow) {
            case "attach-eid":
                try {
                    // Check if necessary data (eid.* and reg id) have been set
                    Address address = (Address) req.getSession().getAttribute("eid.address");
                    Identity identity = (Identity) req.getSession().getAttribute("eid.identity");
                    if (address == null || identity == null || id == null) {
                        Logger.getLogger(getClass()).warn("One or more parameters not present:"
                                + "address: " + address + "\n identity: " + identity
                                + "\n id :" + id);
                        resp.sendError(400, "One or more parameters are not present. \n "
                                + "Please contact Neutrinet for further assistance if this"
                                + "error repeats.");
                    } else {
                        Registration reg = Registration.getActiveRegistrations().get(id);
                        if (reg == null) {
                            resp.sendError(400, "Illegal registration id");
                            return;
                        }

                        User user = reg.user();
                        user.name = identity.getFirstName() + " " + identity.getMiddleName();
                        user.lastName = identity.getName();
                        user.birthPlace = identity.getPlaceOfBirth();
                        user.birthDate = identity.getDateOfBirth().getTime();
                        user.street = address.getStreetAndNumber();
                        user.postalCode = address.getZip();
                        user.municipality = address.getMunicipality();
                        user.certId = identity.chipNumber;

                        try {
                            Users.add(user);

                            reg.createInitialClient();
                            Registrations.dao.update(reg);
                            resp.sendRedirect("/?id=" + id + "&flow=eIdDone");
                        } catch (LDAPPersistException ex) {
                            Logger.getLogger(getClass()).error("Failed to add user", ex);
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).warn("Failed to create user", ex);
                    resp.sendError(500, "Failure");
                }
                break;
            case "confirm-email":
                try {
                    Registration r = Registrations.dao.queryForEq("id", id).get(0);
                    r.client.user().enabled = true;
                    Users.update(r.client.user());

                    resp.sendRedirect("/?id=" + id + "&flow=emailDone");
                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).warn("Failed to find registration", ex);
                    resp.sendError(400, "Illegal registration id");
                }
                break;
        }
    }

}
