package be.neutrinet.ispng.external;

import be.neutrinet.ispng.monitoring.DataPoint;
import org.restlet.resource.Post;

import java.util.List;

/**
 * Created by wannes on 10/2/14.
 */
public interface OpenTSDB {
    @Post
    public void pushData(List<DataPoint> points);
}
