package be.neutrinet.ispng.monitoring;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.external.OpenTSDB;
import org.apache.log4j.Logger;
import org.restlet.resource.ClientResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by wannes on 10/2/14.
 */
public class Agent {
    protected OpenTSDB api;
    protected ConcurrentLinkedQueue<DataPoint> queue;
    public static int MAX_BACKLOG = 1000;
    protected Timer timer;
    protected boolean busy;

    public Agent() {
        if (!VPN.cfg.containsKey("monitoring.opentsdb.uri")) {
            Logger.getLogger(getClass()).warn("OpenTSDB server not configured");
            return;
        }

        String tsdbServer = VPN.cfg.getProperty("monitoring.opentsdb.uri").trim();
        ClientResource cr = new ClientResource(tsdbServer);
        api = cr.wrap(OpenTSDB.class);
        queue = new ConcurrentLinkedQueue<>();
        timer = new Timer("monitorAgent");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pushData();
            }
        }, 0, 1000);

    }

    protected void pushData() {
        if (queue.isEmpty() || busy) return;
        try {
            busy = true;
            ArrayList<DataPoint> points = new ArrayList<>(queue);
            List<DataPoint> pointRange = points.subList(0, 50);
            api.pushData(pointRange);
            for (DataPoint p : pointRange) queue.remove(p);
            busy = false;
        } catch (Exception ex) {
            while (queue.size() > MAX_BACKLOG) {
                queue.remove();
            }
            busy = false;
            Logger.getLogger(getClass()).debug("Failed to push monitoring data", ex);
        }
    }

    public void addDataPoint(DataPoint dp) {
        if (valid(dp) && queue != null) {
            queue.add(dp);
        } else {
            Logger.getLogger(getClass()).warn("Tried to add invalid datapoint");
        }
    }

    protected boolean valid(DataPoint dp) {
        return dp.metric != null && !dp.metric.isEmpty() && dp.timestamp != 0;
    }
}
