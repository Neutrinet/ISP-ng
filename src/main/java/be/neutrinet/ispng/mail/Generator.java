/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.mail;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.IPAddresses;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.UnlockKey;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

            packAndSend(msg, "vpn-confirmation", content);
        } catch (SQLException | MessagingException ex) {
            Logger.getLogger(getClass()).error("Failed to send confirmation", ex);
        }
    }

    public void sendUnlockKey(UnlockKey key, String emailAddress) {
        try {
            MimeMessage msg = this.postman.createNewMessage();
            msg.addRecipients(Message.RecipientType.TO, emailAddress);
            msg.addRecipients(Message.RecipientType.CC, VPN.cfg.getProperty("userManagement.emailAddress"));
            msg.setSubject("Your Neutrinet unlock key");

            HashMap<String, String> content = new HashMap<>();
            content.put("title", "Your unlock key");
            content.put("preview", "Here's your unlock key");
            content.put("key", key.key);

            packAndSend(msg, "unlock-code", content);
        } catch (MessagingException ex) {
            Logger.getLogger(getClass()).error("Failed to send key", ex);
        }
    }

    protected void packAndSend(MimeMessage message, String template, Map<String, String> content) throws MessagingException {
        MimeMultipart multipart = new MimeMultipart("alternative");

        MimeBodyPart plaintext = new MimeBodyPart();
        plaintext.setContent(renderer.renderInTemplate(template, content, true), "text/plain; charset=utf-8");

        MimeBodyPart html = new MimeBodyPart();
        html.setContent(renderer.renderInTemplate(template, content, false), "text/html; charset=utf-8");

        multipart.addBodyPart(plaintext);
        multipart.addBodyPart(html);

        message.setContent(multipart);
        message.setSentDate(new Date());
        postman.sendMessage(message);
    }
}
