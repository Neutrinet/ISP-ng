package be.neutrinet.ispng.openvpn;

import be.neutrinet.ispng.VPN;
import be.neutrinet.ispng.monitoring.DataPoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by wannes on 2/15/15.
 */
public class Monitoring {

    private int activeConnections;
    private HashMap<String, InstanceMetrics> counters = new HashMap<>();
    private TimerTask sample = new TimerTask() {
        @Override
        public void run() {
            for (Map.Entry<String, InstanceMetrics> entry : counters.entrySet()) {
                HashMap<String, String> tags = new HashMap<>();
                tags.put("vpnInstance", entry.getKey());

                DataPoint activeConnections = new DataPoint();
                activeConnections.metric = "activeConnections";
                activeConnections.tags = tags;
                activeConnections.timestamp = System.currentTimeMillis();
                activeConnections.value = entry.getValue().clientMetrics.size();
                VPN.monitoringAgent.addDataPoint(activeConnections);
            }
        }
    };

    private void updateInstanceStats(Client client, String instanceId) {
        InstanceMetrics ic;
        if (!counters.containsKey(instanceId)) {
            ic = new InstanceMetrics();
            counters.put(instanceId, ic);
        } else {
            ic = counters.get(instanceId);
        }

        // remove clients after 10  secs of inactivity
        Iterator<Map.Entry<Integer, ClientMetrics>> it = ic.clientMetrics.entrySet().iterator();
        it.forEachRemaining(entry -> {
            if ((System.currentTimeMillis() - entry.getValue().lastSeen) > 10000)
                it.remove();
        });
    }

    public void byteCount(Client client, long bytesIn, long bytesOut, String instanceId) {
        if (VPN.monitoringAgent == null) return;

        HashMap<String, String> tags = new HashMap<>();
        tags.put("client", "" + client.id);
        tags.put("connection", "" + client.kid);
        tags.put("vpnInstance", instanceId);

        updateInstanceStats(client, instanceId);

        InstanceMetrics ic = counters.get(instanceId);

        ClientMetrics last = ic.clientMetrics.get(client.id);
        if (last == null) {
            last = new ClientMetrics(client.id);
            ic.clientMetrics.put(client.id, last);
        }

        DataPoint bytesInDataPoint = new DataPoint();
        bytesInDataPoint.metric = "vpn.client.bytesIn";
        bytesInDataPoint.timestamp = System.currentTimeMillis();
        bytesInDataPoint.value = bytesIn - last.bytesIn;
        bytesInDataPoint.tags = tags;

        DataPoint bytesOutDataPoint = new DataPoint();
        bytesOutDataPoint.metric = "vpn.client.bytesOut";
        bytesOutDataPoint.timestamp = System.currentTimeMillis();
        bytesOutDataPoint.value = bytesOut - last.bytesOut;
        bytesOutDataPoint.tags = tags;

        last.bytesIn = bytesIn;
        last.bytesOut = bytesOut;

        VPN.monitoringAgent.addDataPoint(bytesInDataPoint);
        VPN.monitoringAgent.addDataPoint(bytesOutDataPoint);
    }

    private static class ClientMetrics {
        int id;
        long bytesIn;
        long bytesOut;
        long lastSeen;

        public ClientMetrics(int id) {
            this.id = id;
        }
    }

    private static class InstanceMetrics {
        HashMap<Integer, ClientMetrics> clientMetrics = new HashMap<>();
    }
}
