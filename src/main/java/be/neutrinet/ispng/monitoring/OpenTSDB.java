package be.neutrinet.ispng.monitoring;

import org.restlet.resource.Post;

import java.util.List;

/**
 * Created by wannes on 10/2/14.
 */
public interface OpenTSDB {
    @Post
    public void pushData(List<DataPoint> points);
}
