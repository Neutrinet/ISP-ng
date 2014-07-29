/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.Registrations;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 *
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
                        reg.user.name = identity.getFirstName() + " " + identity.getMiddleName();
                        reg.user.lastName = identity.getName();
                        reg.user.birthPlace = identity.getPlaceOfBirth();
                        reg.user.birthDate = identity.getDateOfBirth().getTime();
                        reg.user.street = address.getStreetAndNumber();
                        reg.user.postalCode = address.getZip();
                        reg.user.municipality = address.getMunicipality();
                        reg.user.certId = identity.chipNumber;

                        Users.dao.create(reg.user);
                        Registrations.dao.update(reg);
                        resp.sendRedirect("/?id=" + id + "&flow=eIdDone");
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).warn("Failed to create user", ex);
                    resp.sendError(500, "Failure");
                }
                break;
            case "confirm-email":
                try {
                    Registration r = Registrations.dao.queryForEq("id", id).get(0);
                    r.user.enabled = true;
                    Users.dao.update(r.user);

                    resp.sendRedirect("/?id=" + id + "&flow=emailDone");
                } catch (SQLException ex) {
                    Logger.getLogger(getClass()).warn("Failed to find registration", ex);
                    resp.sendError(400, "Illegal registration id");
                }
                break;
        }
    }

}
