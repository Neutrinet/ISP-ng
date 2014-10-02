package be.neutrinet.ispng.monitoring;

import org.restlet.resource.Put;

/**
 * Created by wannes on 10/2/14.
 */
public interface OpenTSDB {
    @Put
    public void pushData(DataPoint[] datapoints);
}
