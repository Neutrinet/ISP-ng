package be.neutrinet.ispng.util;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooKeeper;

/**
 * Created by wannes on 1/30/15.
 */
public class Zookeeper {
    private static CuratorFramework cf;

    public static void boot(String connectionString) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        cf = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
        cf.start();
    }

    public static CuratorFramework get() {
        if (cf == null) throw new IllegalStateException("Zookeeper has not been initialized");
        return cf;
    }

    public static ZooKeeper getZK() throws Exception {
        return get().getZookeeperClient().getZooKeeper();
    }
}
