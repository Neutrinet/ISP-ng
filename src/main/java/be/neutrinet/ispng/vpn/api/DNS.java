package be.neutrinet.ispng.vpn.api;

import be.neutrinet.ispng.dns.DNSRecord;
import be.neutrinet.ispng.dns.DNSRecords;
import be.neutrinet.ispng.dns.ZoneBuilder;
import be.neutrinet.ispng.security.Policy;
import com.j256.ormlite.dao.Dao;
import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Created by wannes on 1/30/15.
 */
public class DNS extends ResourceBase {

    private final static ZoneBuilder builder = new ZoneBuilder();

    public DNS() {
        builder.boot();
    }

    @Get
    public Representation get() {
        if (getQueryValue("zones") != null) {
            return new JacksonRepresentation<>(DNSRecords.getZoneMapping());
        }

        if (!getRequestAttributes().containsKey("zone")) {
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        DNSRecords.getDaoForZone(getAttribute("zone")).orElseThrow(() -> new IllegalArgumentException("Invalid zone"));
        Dao<DNSRecord, String> dao = DNSRecords.getDaoForZone(getAttribute("zone")).get();

        try {
            return new JacksonRepresentation<>(
                    dao.queryForAll()
                            .parallelStream()
                            .filter((record) -> Policy.get().canModify(getLoggedInUser(), record))
                            .collect(Collectors.toList())
            );
        } catch (SQLException ex) {
            Logger.getLogger(getClass()).error("Failed to get user DNS records", ex);
        }

        return DEFAULT_ERROR;
    }

    @Put
    public Representation create(DNSRecord record) {
        if (record == null) return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        if (!getRequestAttributes().containsKey("zone")) {
            return clientError("INVALID_REQUEST", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        builder.addOrUpdate(record, getAttribute("zone"));

        return DEFAULT_SUCCESS;
    }
}
