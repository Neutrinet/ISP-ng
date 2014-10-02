package be.neutrinet.ispng.monitoring;

import be.neutrinet.ispng.VPN;
import org.apache.log4j.Logger;
import org.restlet.resource.ClientResource;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by wannes on 10/2/14.
 */
public class Agent {
    protected OpenTSDB api;
    protected ConcurrentLinkedQueue<DataPoint> queue;
    protected Timer timer;

    public Agent() {
        if (!VPN.cfg.containsKey("monitoring.opentsdb.server")) {
            Logger.getLogger(getClass()).warn("OpenTSDB server not configured");
            return;
        }

        String tsdbServer = VPN.cfg.getProperty("monitoring.opentsdb.server");
        ClientResource cr = new ClientResource(tsdbServer);
        api = cr.wrap(OpenTSDB.class);
        queue = new ConcurrentLinkedQueue<>();
        timer = new Timer("monitorAgent");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pushData();
            }
        }, 0, 100);

    }

    protected void pushData() {
        try {
            DataPoint[] points = queue.toArray(new DataPoint[]{});
            api.pushData(points);
            for (DataPoint p : points) queue.remove(p);
        } catch (Exception ex) {

        }
    }

    public void addDataPoint(DataPoint dp) {
        if (dp.isValid() && queue != null) {
            queue.add(dp);
        } else {
            Logger.getLogger(getClass()).warn("Tried to add invalid datapoint");
        }
    }
}
