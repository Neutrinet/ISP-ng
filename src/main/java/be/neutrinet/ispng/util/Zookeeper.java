package be.neutrinet.ispng.util;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

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

    public static void ensurePathExists(String path) {
        try {
            Stat stat = cf.checkExists().forPath(path);
            if (stat == null)
                cf.create().creatingParentsIfNeeded().forPath(path, null);
        } catch (Exception ex) {
            Logger.getLogger(Zookeeper.class).error("Failed to create path " + path, ex);
        }
    }
}
