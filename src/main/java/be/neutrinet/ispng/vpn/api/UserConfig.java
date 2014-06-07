/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.ClientError;
import be.neutrinet.ispng.vpn.ResourceBase;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.admin.Registration;
import be.neutrinet.ispng.vpn.admin.Registrations;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restlet.data.CharacterSet;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

/**
 *
 * @author wannes
 */
public class UserConfig extends ResourceBase {

    public final static String[] DEFAULTS = new String[]{
        "client",
        "dev tap",
        "proto udp",
        "remote " + VPN.cfg.getProperty("openvpn.publicaddress") + ' ' + VPN.cfg.getProperty("openvpn.publicport"),
        "resolv-retry infinite",
        "nobind",
        "ns-cert-type server",
        "comp-lzo",
        "ca ca.crt",
        "auth-user-pass"
    };

    public static byte[] caCert;

    @Post
    public Representation getConfig(Map<String, String> data) {
        try {
            Registration r = Registrations.dao.queryForEq("id", UUID.fromString(data.get("regId"))).get(0);
            if (r == null) {
                return new JacksonRepresentation(new ClientError("INVALID_REG_ID"));
            }
            User user = r.user;

            ArrayList<String> config = new ArrayList<>();
            config.addAll(Arrays.asList(DEFAULTS));
            switch (data.get("platform").toLowerCase()) {
                case "linux":
                    config.add("pkcs11-providers /usr/lib/libbeidpkcs11.so");
                    config.add("pkcs11-id \"Belgium Government/Belgium eID/" + user.certId.substring(16) + "/BELPIC/0200000000000000\"");
                    break;
                case "osx":
                    config.add("pkcs11-providers /usr/local/lib/beid-pkcs11.bundle/Contents/MacOS/libbeidpkcs11.dylib");
                    config.add("pkcs11-id \"Belgium Government/Belgium eID/" + user.certId.substring(16) + "/BELPIC/0200000000000000\"");
                    break;
                case "windows":
                    config.add("pkcs11-providers C:\\\\WINDOWS\\\\system32\\\\beidpkcs11.dll");
                    config.add("pkcs11-id \"Belgium Government/Belgium eID/" + user.certId.substring(16) + "/BELPIC/02000000\"");
                    break;
                default:
                    return new JacksonRepresentation(new ClientError("ILLEGAL_PLATFORM_TYPE"));
            }

            String file = "";
            for (String s : config) {
                file += s + '\n';
            }

            if (caCert == null) {
                caCert = IOUtils.toByteArray(new FileInputStream(VPN.cfg.getProperty("ca.crt")));
            }

            // create zip
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(baos);

            ZipEntry configFile = new ZipEntry("neutrinet.ovpn");
            configFile.setCreationTime(FileTime.from(Instant.now()));
            zip.putNextEntry(configFile);
            zip.write(file.getBytes());
            zip.putNextEntry(new ZipEntry("ca.crt"));
            zip.write(caCert);
            zip.close();

            ByteArrayRepresentation rep = new ByteArrayRepresentation(baos.toByteArray());
            rep.setMediaType(MediaType.APPLICATION_ZIP);
            rep.setSize(baos.size());
            rep.setCharacterSet(CharacterSet.UTF_8);
            rep.setDisposition(new Disposition(Disposition.TYPE_ATTACHMENT));
            return rep;
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to generate config file archive", ex);
        }

        return DEFAULT_ERROR;
    }
}
