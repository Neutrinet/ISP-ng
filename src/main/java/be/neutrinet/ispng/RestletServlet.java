/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng;

import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.api.*;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletAdapter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.SecretVerifier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

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

        // Mind the order - most specific routes first
        Router router = new Router(this.adapter.getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
        router.attach("/reg/{id}", UserRegistration.class);
        router.attach("/address/pool/{id}", AddressPool.class);
        router.attach("/address/lease/{id}", AddressLease.class);
        router.attach("/unlock-key/{key}", UnlockKey.class);
        router.attach("/connection/{id}", VPNConnections.class);
        router.attach("/client/{client}/cert/{cert}", VPNClientCertificate.class);
        router.attach("/client/{client}", VPNClient.class);
        router.attach("/user/{user}/config", UserVPNClientConfig.class);
        router.attach("/user/{user}/setting/{setting}", UserSettings.class);
        router.attach("/user/{user}", UserManagement.class);
        router.attach("/user/login", UserLogin.class);

        ChallengeAuthenticator auth = new ChallengeAuthenticator(this.adapter.getContext(), ChallengeScheme.HTTP_BASIC, "Neutrinet API") {

            @Override
            protected int beforeHandle(Request request, Response response) {

                if (request.getResourceRef().getPath().startsWith("/api/reg/")) {
                    response.setStatus(Status.SUCCESS_OK);
                    return CONTINUE;
                }

                if (request.getCookies().getFirst("Registration-ID") != null) {
                    UUID id = UUID.fromString(request.getCookies().getFirstValue("Registration-ID"));
                    if (Registration.getActiveRegistrations().containsKey(id)) {
                        response.setStatus(Status.SUCCESS_OK);
                        return CONTINUE;
                    }
                }

                return super.beforeHandle(request, response);
            }

        };

        auth.setVerifier(new SecretVerifier() {

            @Override
            public int verify(String identifier, char[] secret) {
                User user = Users.authenticate(identifier, new String(secret));
                if (user != null) {
                    return RESULT_VALID;
                } else {
                    return RESULT_INVALID;
                }
            }
        });
        auth.setNext(router);

        this.adapter.setNext(auth);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        this.adapter.service(req, res);
    }
}
