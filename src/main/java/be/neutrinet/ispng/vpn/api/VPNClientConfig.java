/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.security.Policy;
import be.neutrinet.ispng.vpn.*;
import be.neutrinet.ispng.vpn.ca.Certificate;
import be.neutrinet.ispng.vpn.ca.Certificates;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.restlet.data.CharacterSet;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author wannes
 */
public class VPNClientConfig extends ResourceBase {

    public final static String[] DEFAULTS = new String[]{
            "client",
            "dev tun",
            "proto udp",
            "remote " + VPN.cfg.getProperty("openvpn.publicaddress") + ' ' + VPN.cfg.getProperty("openvpn.publicport"),
            "resolv-retry infinite",
            "nobind",
            "ns-cert-type server",
            "comp-lzo",
            "ca ca.crt",
            "auth-user-pass",
            "topology subnet"
    };

    public static byte[] caCert;

    @Get
    public Representation getPKCS11Config() {
        if (getQueryValue("user") != null && getQueryValue("platform") != null) {
            String userId = getQueryValue("user");
            String platform = getQueryValue("platform");

            try {
                User user = Users.queryForId(userId);
                if (user.certId == null) return clientError("NO_KEYPAIR", Status.CLIENT_ERROR_FAILED_DEPENDENCY);

                ArrayList<String> config = new ArrayList<>();
                config.addAll(Arrays.asList(DEFAULTS));

                // create zip
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(baos);

                Representation res = addPKCS11config(platform.toLowerCase(), config, user);
                if (res != null) return res;

                return finalizeZip(config, zip, baos);
            } catch (Exception ex) {
                Logger.getLogger(getClass()).debug("Failed to build PKCS11 config for user " + userId, ex);
            }
        }

        return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
    }

    @Post
    public Representation getConfig(Map<String, String> data) {
        try {
            if (!getRequestAttributes().containsKey("client"))
                return clientError("MALFORMED_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);

            Client client = Clients.dao.queryForId(getAttribute("client"));
            if (!Policy.get().canAccess(getSessionToken().get().getUser(), client)) {
                return clientError("FORBIDDEN", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            ArrayList<String> config = new ArrayList<>();
            config.addAll(Arrays.asList(DEFAULTS));

            // create zip
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(baos);

            List<Certificate> userCert = Certificates.dao.queryForEq("client_id", client.id).stream()
                    .filter(Certificate::valid).collect(Collectors.toList());

            if (!userCert.isEmpty()) {
                byte[] raw = userCert.get(0).getRaw();
                zip.putNextEntry(new ZipEntry("client.crt"));
                zip.write(raw);
                zip.putNextEntry(new ZipEntry("README"));
                zip.write(("!! You are using your own keypair. Please make sure to adjust the "
                        + "path to your private key in the config file or move the private key"
                        + " here and name it client.key").getBytes());
                config.add("cert client.crt");
                config.add("key client.key");
            } else if (client.user().certId != null && !client.user().certId.isEmpty()) {
                Representation res = addPKCS11config(data.get("platform").toLowerCase(), config, client.user());
                if (res != null) return res;
            }

            if (client.user().certId == null || client.user().certId.isEmpty()) {
                zip.putNextEntry(new ZipEntry("NO_KEYPAIR_DEFINED"));
                zip.write("Invalid state, no keypair has been defined.".getBytes());
            }

            return finalizeZip(config, zip, baos);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to generate config file archive", ex);
        }

        return DEFAULT_ERROR;
    }

    protected Representation finalizeZip(List<String> config, ZipOutputStream zip, ByteArrayOutputStream baos) throws Exception {
        String file = "";
        for (String s : config) {
            file += s + '\n';
        }

        if (caCert == null) {
            caCert = IOUtils.toByteArray(new FileInputStream(VPN.cfg.getProperty("ca.crt")));
        }

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
    }

    protected Representation addPKCS11config(String platform, List<String> config, User user) {
        switch (platform) {
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

        return null;
    }
}
