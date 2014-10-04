package be.neutrinet.ispng.monitoring;

import java.util.Map;

/**
 * Created by wannes on 10/2/14.
 */
public class DataPoint {
    public String metric;
    public long timestamp;
    public double value;
    public Map<String, String> tags;

}
