/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.security.SessionManager;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.api.*;
import be.neutrinet.ispng.vpn.api.DNS;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsResponseHelper;
import org.restlet.ext.servlet.ServletAdapter;
import org.restlet.routing.Filter;
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
        router.attach("/subnet/lease/{id}", SubnetLease.class);
        router.attach("/unlock-key/{key}", UnlockKey.class);
        router.attach("/client/{client}/connection/{id}", VPNConnections.class);
        router.attach("/client/{client}/cert/{cert}", VPNClientCertificate.class);
        router.attach("/client/{client}/config", VPNClientConfig.class);
        router.attach("/client/{client}", VPNClient.class);
        router.attach("/user/{user}/setting/{setting}", UserSettings.class);
        router.attach("/user/login", UserLogin.class);
        router.attach("/user/{user}", UserManagement.class);
        router.attach("/dns/{zone}", DNS.class);

        ChallengeAuthenticator auth = new ChallengeAuthenticator(this.adapter.getContext(), ChallengeScheme.HTTP_BASIC, "Neutrinet API") {

            @Override
            protected int beforeHandle(Request request, Response response) {
                if (request.getResourceRef().getPath().startsWith("/api/reg/") ||
                        request.getResourceRef().getPath().startsWith("/api/user/login")) {
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
            private User user = null;

            @Override
            public int verify(Request request, Response response) {
                int result = super.verify(request, response);

                String sessionToken = null;
                if (request.getCookies().getFirst("Session") != null) {
                    sessionToken = request.getCookies().getFirstValue("Session");
                } else if (request.getHeaders().getFirstValue("Session") != null) {
                    sessionToken = request.getHeaders().getFirstValue("Session");
                }

                if (sessionToken != null) {
                    if (!SessionManager.validateToken(sessionToken, request.getClientInfo().getAddress())) {
                        response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                        return RESULT_INVALID;
                    } else {
                        return RESULT_VALID;
                    }
                } else {
                    assert user != null;
                    response.setLocationRef("login");
                }

                return result;
            }

            @Override
            public int verify(String identifier, char[] secret) {
                user = Users.authenticate(identifier, new String(secret));
                if (user != null) {
                    return RESULT_VALID;
                } else {
                    return RESULT_INVALID;
                }
            }
        });
        auth.setNext(router);

        Filter corsFilter = new Filter(this.adapter.getContext()) {
            @Override
            protected int beforeHandle(Request request, Response response) {
                response.getAllowedMethods().add(Method.DELETE);
                response.getAllowedMethods().add(Method.GET);
                response.getAllowedMethods().add(Method.HEAD);
                response.getAllowedMethods().add(Method.POST);
                response.getAllowedMethods().add(Method.PUT);

                // F*CK ME CORS FINALLY WORKS
                // WORST DOC EVER
                CorsResponseHelper corsResponseHelper = new CorsResponseHelper();
                corsResponseHelper.allowedCredentials = true;
                corsResponseHelper.allowAllRequestedHeaders = true;
                corsResponseHelper.addCorsResponseHeaders(request, response);

                if (request.getMethod().equals(Method.OPTIONS)) {
                    return STOP;
                }

                return CONTINUE;
            }
        };
        corsFilter.setNext(auth);

        this.adapter.setNext(corsFilter);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        this.adapter.service(req, res);
    }
}
