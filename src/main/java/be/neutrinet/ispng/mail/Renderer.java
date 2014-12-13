/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.mail;

import be.neutrinet.ispng.VPN;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wannes
 */
public class Renderer {

    private final static String BASE_TEMPLATE = "template";

    public Renderer() {

    }

    public String renderInTemplate(String segmentName, Map<String, String> content, boolean plaintext) {
        fillInDefaults(content);
        
        String rseg = render(segmentName, content, plaintext);
        HashMap<String, String> ct = new HashMap<>();
        content.remove("body");
        ct.putAll(content);
        ct.put("body", rseg);
        
        return render(BASE_TEMPLATE, ct, plaintext);
    }
    
    protected void fillInDefaults(Map<String, String> content) {
        if (!content.containsKey("header-img-src")) {
            content.put("header-img-src", VPN.cfg.getProperty("mail.headerImageURL"));
            content.put("header-img-alt", VPN.cfg.getProperty("mail.headerImageAlt"));
            content.put("base-url", "https://" + VPN.cfg.getProperty("service.hostname")
                    + ":" + VPN.cfg.getProperty("service.port"));
        }
    }

    public String render(String segmentName, Map<String, String> content, boolean plaintext) {
        try {
            String segment = IOUtils.toString(new FileReader("web/mail/" +
                    (plaintext ? "plaintext" : "html") + '/' + segmentName + (plaintext ? ".txt" : ".html")));
            int idx = segment.indexOf("[%");

            String rendered;
            if (idx == -1) {
                rendered = segment;
            } else {
                rendered = segment.substring(0, idx);

                while (true) {
                    int cls = segment.indexOf("%]", idx);
                    String key = segment.substring(idx + 2, cls).trim();
                    if (content.containsKey(key)) {
                        rendered += content.get(key);
                    }
                    idx = segment.indexOf("[%", idx + 2);
                    int endOfThingy = cls + 2;
                    if (idx < 0) {
                        rendered += segment.substring(endOfThingy);
                        break;
                    } else {
                        rendered += segment.substring(endOfThingy, idx);
                    }
                }
            }

            return rendered;
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to read mail template", ex);
        }

        return null;
    }
}
