/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.neutrinet.ispng;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.admin.Registration;
import java.io.IOException;
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
        // Check if necessary data (eid.* and reg id) have been set
        Address address = (Address) req.getSession().getAttribute("eid.address");
        Identity identity = (Identity) req.getSession().getAttribute("eid.identity");
        UUID id = UUID.fromString(req.getQueryString().substring(req.getQueryString().indexOf('=')+1));
        
        if (address == null || identity == null || id == null) {
            Logger.getLogger(getClass()).warn("One or more parameters not present:"
                    + "address: " + address + "\n identity: " + identity +
                    "\n id :" + id);
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
            reg.user.postalCode = Integer.parseInt(address.getZip());
            reg.user.municipality = address.getMunicipality();
            
            resp.sendRedirect("/?id=" + id + "&flow=eIdDone");
        }
    }
    
}
