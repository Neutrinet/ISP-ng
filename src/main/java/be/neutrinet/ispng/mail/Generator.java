/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.mail;

import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import be.neutrinet.ispng.vpn.admin.Registration;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;

/**
 *
 * @author wannes
 */
public class Generator {

    protected Postman postman;
    protected Renderer renderer;

    public Generator() {
        this.postman = new Postman();
        this.renderer = new Renderer();
    }

    public void sendRegistrationConfirmation(Registration r) {
        try {
            MimeMessage msg = this.postman.createNewMessage();
            msg.addRecipients(Message.RecipientType.TO, r.user.email);
            msg.setSubject("Registration confirmation");

            HashMap<String, String> content = new HashMap<>();
            content.put("title", "Confirmation");
            content.put("preview", "You successfully created your Neutrinet account");
            content.put("email", r.user.email);
            content.put("name", r.user.name + " " + r.user.lastName);
            content.put("reg-id", r.getId().toString());
            if (r.ipv4Id != 0) {
                content.put("ipv4", IPAddresses.dao.queryForId("" + r.ipv4Id).address);
            } else {
                content.put("ipv4", "No IPv4 address");
            }
            if (r.ipv6Id != 0) {
                content.put("ipv6", IPAddresses.dao.queryForId("" + r.ipv6Id).address);
            } else {
                content.put("ipv6", "No IPv6 subnet");
            }

            String body = renderer.renderInTemplate("vpn-confirmation", content);
            msg.setContent(body, "text/html; charset=utf-8");
            msg.setSentDate(new Date());
            postman.sendMessage(msg);
        } catch (SQLException | MessagingException ex) {
            Logger.getLogger(getClass()).error("Failed to send confirmation", ex);
        }
    }
}