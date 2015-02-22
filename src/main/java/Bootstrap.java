import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.vpn.IPAddress;
import be.neutrinet.ispng.vpn.IPAddresses;
import be.neutrinet.ispng.vpn.User;
import be.neutrinet.ispng.vpn.Users;

import java.util.Date;

/**
 * Created by wannes on 1/17/15.
 */
public class Bootstrap {
    public static void main(String[] args) throws Exception {
        VPN vpn = new VPN();
        vpn.init(null);

        User user = new User();
        user.birthDate = new Date();
        user.birthPlace = "Spaaaace!";
        user.country = "Botswana";
        user.email = "boot@strap.be";
        user.lastName = "Bobson";
        user.name = "Alice";
        user.municipality = "Moonbase A6-9K";
        user.postalCode = "42";
        user.setPassword("password");
        Users.dao.create(user);

        IPAddresses.addv4SubnetToPool("192.168.221.129/25", IPAddress.Purpose.CLIENT_ASSIGN);
    }
}
