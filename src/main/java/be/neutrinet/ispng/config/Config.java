package be.neutrinet.ispng.config;

import be.neutrinet.ispng.VPN;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by wannes on 24/08/14.
 */
public class Config {
    private final static Config instance = new Config();
    private final static Charset CHARSET = Charset.forName("UTF-8");
    private final static String PREFIX = "/ispng/";
    private CuratorFramework cf;

    private Config() {

    }

    public static Config get() {
        return instance;
    }

    public static Optional<String> get(String key) {
        return instance.getValue(key);
    }

    public static String get(String key, String defaultValue) {
        Optional<String> val = get(key);
        if (val.isPresent()) return val.get();
        else return defaultValue;
    }

    public final void boot() {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        cf = CuratorFrameworkFactory.newClient(VPN.cfg.getProperty("zookeeper.connectionString").toString(), retryPolicy);
        cf.start();

        for (Map.Entry<Object, Object> entry : VPN.cfg.entrySet()) {
            write(entry.getKey().toString().replace(".", "/"), entry.getValue().toString());
        }
    }

    public Optional<String> getValue(String key) {
        try {
            Stat stat = cf.checkExists().forPath(PREFIX + key);
            if (stat == null) return Optional.empty();
            byte[] value = cf.getData().forPath(PREFIX + key);
            if (value == null) return Optional.empty();
            return Optional.ofNullable(new String(value, CHARSET));
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to read config from ZeeKeeper", ex);
        }

        return Optional.empty();
    }

    public void write(String key, String value) {
        try {
            Stat stat = cf.checkExists().forPath(PREFIX + key);
            if (stat == null) {
                cf.create().creatingParentsIfNeeded().forPath(PREFIX + key, value.getBytes(CHARSET));
            } else {
                cf.setData().forPath(PREFIX + key, value.getBytes(CHARSET));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to write config to ZooKeeper", ex);
        }
    }

    public void getAndWatch(String key, String defaultValue, Consumer<String> listener) {
        try {
            CuratorWatcher cw = (WatchedEvent watchedEvent) -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    byte[] value = cf.getData().forPath(watchedEvent.getPath());
                    listener.accept(new String(value, CHARSET));
                }
            };

            Stat stat = cf.checkExists().forPath(PREFIX + key);
            if (stat == null)
                cf.create().creatingParentsIfNeeded().forPath(PREFIX + key, defaultValue.getBytes(CHARSET));
            cf.getData().usingWatcher(cw).forPath(PREFIX + key);
            listener.accept(get(key, defaultValue));
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to add config watcher", ex);
        }
    }

    public void watch(String key, Consumer<String> listener) {
        try {
            CuratorWatcher cw = (WatchedEvent watchedEvent) -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    byte[] value = cf.getData().forPath(watchedEvent.getPath());
                    listener.accept(new String(value, CHARSET));
                }
            };

            Stat stat = cf.checkExists().forPath(PREFIX + key);
            if (stat == null)
                cf.create().creatingParentsIfNeeded().forPath(PREFIX + key, null);
            cf.getData().usingWatcher(cw).forPath(PREFIX + key);
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to add config watcher", ex);
        }
    }

    public void watch(String[] keys, Consumer<String> listener) {
        try {
            CuratorWatcher cw = (WatchedEvent watchedEvent) -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    byte[] value = cf.getData().forPath(watchedEvent.getPath());
                    listener.accept(new String(value, CHARSET));
                }
            };

            for (String key : keys) {
                Stat stat = cf.checkExists().forPath(PREFIX + key);
                if (stat == null)
                    cf.create().creatingParentsIfNeeded().forPath(PREFIX + key, null);
                cf.getData().usingWatcher(cw).forPath(PREFIX + key);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass()).error("Failed to add config watcher", ex);
        }
    }
}
