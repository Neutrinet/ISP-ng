package be.neutrinet.ispng.dns;

import be.neutrinet.ispng.DNS;
import org.apache.log4j.Logger;
import org.xbill.DNS.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wannes on 1/25/15.
 */
public class RequestHandler {

    static final int FLAG_DNSSECOK = 1;
    static final int FLAG_SIGONLY = 2;
    private Map<Integer, Cache> caches = new HashMap<>();

    public Cache getCache(int dclass) {
        Cache c = caches.get(dclass);
        if (c == null) {
            c = new Cache(dclass);
            caches.put(dclass, c);
        }
        return c;
    }

    public Zone findBestZone(Name name) {
        Zone foundzone = null;
        foundzone = DNS.zones.get(name);
        if (foundzone != null)
            return foundzone;
        int labels = name.labels();
        for (int i = 1; i < labels; i++) {
            Name tname = new Name(name, i);
            foundzone = DNS.zones.get(tname);
            if (foundzone != null)
                return foundzone;
        }
        return null;
    }

    public RRset findExactMatch(Name name, int type, int dclass, boolean glue) {
        Zone zone = findBestZone(name);
        if (zone != null)
            return zone.findExactMatch(name, type);
        else {
            RRset[] rrsets;
            Cache cache = getCache(dclass);
            if (glue)
                rrsets = cache.findAnyRecords(name, type);
            else
                rrsets = cache.findRecords(name, type);
            if (rrsets == null)
                return null;
            else
                return rrsets[0]; /* not quite right */
        }
    }

    void addRRset(Name name, Message response, RRset rrset, int section, int flags) {
        for (int s = 1; s <= section; s++)
            if (response.findRRset(name, rrset.getType(), s))
                return;
        if ((flags & FLAG_SIGONLY) == 0) {
            Iterator it = rrset.rrs();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (r.getName().isWild() && !name.isWild())
                    r = r.withName(name);
                response.addRecord(r, section);
            }
        }
        if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
            Iterator it = rrset.sigs();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (r.getName().isWild() && !name.isWild())
                    r = r.withName(name);
                response.addRecord(r, section);
            }
        }
    }

    private final void addSOA(Message response, Zone zone) {
        response.addRecord(zone.getSOA(), Section.AUTHORITY);
    }

    private final void addNS(Message response, Zone zone, int flags) {
        RRset nsRecords = zone.getNS();
        addRRset(nsRecords.getName(), response, nsRecords,
                Section.AUTHORITY, flags);
    }

    private final void addCacheNS(Message response, Cache cache, Name name) {
        SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
        if (!sr.isDelegation())
            return;
        RRset nsRecords = sr.getNS();
        Iterator it = nsRecords.rrs();
        while (it.hasNext()) {
            Record r = (Record) it.next();
            response.addRecord(r, Section.AUTHORITY);
        }
    }

    private void addGlue(Message response, Name name, int flags) {
        RRset a = findExactMatch(name, Type.A, DClass.IN, true);
        if (a == null)
            return;
        addRRset(name, response, a, Section.ADDITIONAL, flags);
    }

    private void addAdditional2(Message response, int section, int flags) {
        Record[] Records = response.getSectionArray(section);
        for (Record r : Records) {
            Name glueName = r.getAdditionalName();
            if (glueName != null)
                addGlue(response, glueName, flags);
        }
    }

    private final void addAdditional(Message response, int flags) {
        addAdditional2(response, Section.ANSWER, flags);
        addAdditional2(response, Section.AUTHORITY, flags);
    }

    byte addAnswer(Message response, Name name, int type, int dclass,
                   int iterations, int flags) {
        SetResponse sr;
        byte rcode = Rcode.NOERROR;

        if (iterations > 6)
            return Rcode.NOERROR;

        if (type == Type.SIG || type == Type.RRSIG) {
            type = Type.ANY;
            flags |= FLAG_SIGONLY;
        }

        Zone zone = findBestZone(name);
        if (zone != null)
            sr = zone.findRecords(name, type);
        else {
            Cache cache = getCache(dclass);
            sr = cache.lookupRecords(name, type, Credibility.NORMAL);
        }

        if (sr.isUnknown()) {
            addCacheNS(response, getCache(dclass), name);
        }
        if (sr.isNXDOMAIN()) {
            response.getHeader().setRcode(Rcode.NXDOMAIN);
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            }
            rcode = Rcode.NXDOMAIN;
        } else if (sr.isNXRRSET()) {
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            }
        } else if (sr.isDelegation()) {
            RRset nsRecords = sr.getNS();
            addRRset(nsRecords.getName(), response, nsRecords,
                    Section.AUTHORITY, flags);
        } else if (sr.isCNAME()) {
            CNAMERecord cname = sr.getCNAME();
            RRset rrset = new RRset(cname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0)
                response.getHeader().setFlag(Flags.AA);
            rcode = addAnswer(response, cname.getTarget(),
                    type, dclass, iterations + 1, flags);
        } else if (sr.isDNAME()) {
            DNAMERecord dname = sr.getDNAME();
            RRset rrset = new RRset(dname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            Name newname;
            try {
                newname = name.fromDNAME(dname);
            } catch (NameTooLongException e) {
                return Rcode.YXDOMAIN;
            }
            rrset = new RRset(new CNAMERecord(name, dclass, 0, newname));
            addRRset(name, response, rrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0)
                response.getHeader().setFlag(Flags.AA);
            rcode = addAnswer(response, newname, type, dclass,
                    iterations + 1, flags);
        } else if (sr.isSuccessful()) {
            RRset[] rrsets = sr.answers();
            for (RRset rrset : rrsets)
                addRRset(name, response, rrset,
                        Section.ANSWER, flags);
            if (zone != null) {
                addNS(response, zone, flags);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            } else
                addCacheNS(response, getCache(dclass), name);
        }
        return rcode;
    }

    protected final byte[] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
        Zone zone = DNS.zones.get(name);
        boolean first = true;
        if (zone == null)
            return errorMessage(query, Rcode.REFUSED);
        Iterator it = zone.AXFR();
        try {
            DataOutputStream dataOut;
            dataOut = new DataOutputStream(s.getOutputStream());
            int id = query.getHeader().getID();
            while (it.hasNext()) {
                RRset rrset = (RRset) it.next();
                Message response = new Message(id);
                Header header = response.getHeader();
                header.setFlag(Flags.QR);
                header.setFlag(Flags.AA);
                addRRset(rrset.getName(), response, rrset,
                        Section.ANSWER, FLAG_DNSSECOK);
                if (tsig != null) {
                    tsig.applyStream(response, qtsig, first);
                    qtsig = response.getTSIG();
                }
                first = false;
                byte[] out = response.toWire();
                dataOut.writeShort(out.length);
                dataOut.write(out);
            }

            Logger.getLogger(getClass()).info("AXFR of " + zone.getOrigin().toString() + " to " + s.getInetAddress().getCanonicalHostName() + " completed");
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("AXFR of " + zone.getOrigin().toString() + " failed", ex);
        }
        try {
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(getClass()).error("Failed to service AXFR", ex);
        }
        return null;
    }

    /*
     * Note: a null return value means that the caller doesn't need to do
     * anything.  Currently this only happens if this is an AXFR request over
     * TCP.
     */
    byte[] generateReply(Message query, byte[] in, int length, Socket s) throws IOException {
        Header header;
        boolean badversion;
        int maxLength;
        int flags = 0;

        header = query.getHeader();
        if (header.getFlag(Flags.QR))
            return null;
        if (header.getRcode() != Rcode.NOERROR)
            return errorMessage(query, Rcode.FORMERR);
        if (header.getOpcode() != Opcode.QUERY)
            return errorMessage(query, Rcode.NOTIMP);

        Record queryDNSRecord = query.getQuestion();

        TSIGRecord queryTSIG = query.getTSIG();
        TSIG tsig = null;
        if (queryTSIG != null) {
            tsig = DNS.TSIG.get(queryTSIG.getName().toString());
            if (tsig == null || tsig.verify(query, in, length, null) != Rcode.NOERROR) {
                Logger.getLogger(getClass()).warn("Invalid TSIG key from " + s.getInetAddress().getCanonicalHostName());
                return formerrMessage(in);
            }
        } else {
            Logger.getLogger(getClass()).warn("DNS query without TSIG received from " + s.getInetAddress().getCanonicalHostName());
            return formerrMessage(in);
        }

        OPTRecord queryOPT = query.getOPT();
        if (queryOPT != null && queryOPT.getVersion() > 0)
            badversion = true;

        if (s != null)
            maxLength = 65535;
        else if (queryOPT != null)
            maxLength = Math.max(queryOPT.getPayloadSize(), 512);
        else
            maxLength = 512;

        if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0)
            flags = FLAG_DNSSECOK;

        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        if (query.getHeader().getFlag(Flags.RD))
            response.getHeader().setFlag(Flags.RD);
        response.addRecord(queryDNSRecord, Section.QUESTION);

        Name name = queryDNSRecord.getName();
        int type = queryDNSRecord.getType();
        int dclass = queryDNSRecord.getDClass();
        if (type == Type.AXFR && s != null)
            return doAXFR(name, query, tsig, queryTSIG, s);
        if (!Type.isRR(type) && type != Type.ANY)
            return errorMessage(query, Rcode.NOTIMP);

        byte rcode = addAnswer(response, name, type, dclass, 0, flags);
        if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN)
            return errorMessage(query, rcode);

        addAdditional(response, flags);

        if (queryOPT != null) {
            int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
            OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0,
                    optflags);
            response.addRecord(opt, Section.ADDITIONAL);
        }

        response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
        return response.toWire(maxLength);
    }

    byte[] buildErrorMessage(Header header, int rcode, Record question) {
        Message response = new Message();
        response.setHeader(header);
        for (int i = 0; i < 4; i++)
            response.removeAllRecords(i);
        if (rcode == Rcode.SERVFAIL)
            response.addRecord(question, Section.QUESTION);
        header.setRcode(rcode);
        return response.toWire();
    }

    public byte[] formerrMessage(byte[] in) {
        Header header;
        try {
            header = new Header(in);
        } catch (IOException e) {
            return null;
        }
        return buildErrorMessage(header, Rcode.FORMERR, null);
    }

    public byte[] errorMessage(Message query, int rcode) {
        return buildErrorMessage(query.getHeader(), rcode,
                query.getQuestion());
    }
}
