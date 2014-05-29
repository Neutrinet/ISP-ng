/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng;

import be.neutrinet.ispng.vpn.api.AddressLease;
import be.neutrinet.ispng.vpn.api.AddressPool;
import be.neutrinet.ispng.vpn.api.UserRegistration;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.restlet.ext.servlet.ServletAdapter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 *
 * @author wannes
 */
public class RestletServlet extends HttpServlet {

    private ServletAdapter adapter;

    @Override
    public void init() throws ServletException {
        super.init();
        this.adapter = new ServletAdapter(getServletContext());

        Router router = new Router(this.adapter.getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
        router.attach("/reg/{id}", UserRegistration.class);
        router.attach("/address/lease", AddressLease.class);
        router.attach("/address/pool", AddressPool.class);

        this.adapter.setNext(router);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        this.adapter.service(req, res);
    }
}
