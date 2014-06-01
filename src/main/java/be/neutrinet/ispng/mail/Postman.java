// http://stackoverflow.com/questions/3649014/send-email-using-java
package be.neutrinet.ispng.mail;

import be.neutrinet.ispng.VPN;
import com.sun.mail.smtp.SMTPTransport;
import java.security.Security;
import java.util.Date;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author wannes
 */
public class Postman {

    private final String user;
    private final String password;
    private Session session;

    public Postman() {
        this.user = VPN.cfg.getProperty("smtp.user");
        this.password = VPN.cfg.getProperty("smtp.password");
        assert user != null && password != null;
    }

    public void init() {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

        // Get a Properties object
        Properties props = System.getProperties();
        props.setProperty("mail.smtps.host", VPN.cfg.getProperty("smtp.server"));
        props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.port", "465");
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtps.auth", "true");

        /*
         If set to false, the QUIT command is sent and the connection is immediately closed. If set 
         to true (the default), causes the transport to wait for the response to the QUIT command.

         ref :   http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
         http://forum.java.sun.com/thread.jspa?threadID=5205249
         smtpsend.java - demo program from javamail
         */
        props.put("mail.smtps.quitwait", "false");

        session = Session.getInstance(props, null);
    }

    /**
     * Create MimeMessage using Gandi session
     *
     * @return MimeMessage created message
     * @throws AddressException if the email address parse failed
     * @throws MessagingException if the connection is dead or not in the
     * connected state or if the message is not a MimeMessage
     */
    public MimeMessage createNewMessage() throws AddressException, MessagingException {
        if (session == null) {
            init();
        }
        return new MimeMessage(session);
    }

    public void sendMessage(final MimeMessage msg) throws MessagingException {
        // -- Set the FROM and TO fields --
        msg.setFrom(new InternetAddress(user));
        msg.setSentDate(new Date());

        SMTPTransport t = (SMTPTransport) session.getTransport("smtps");

        t.connect(VPN.cfg.getProperty("smtp.server"), user, password);
        t.sendMessage(msg, msg.getAllRecipients());
        t.close();
    }
}
