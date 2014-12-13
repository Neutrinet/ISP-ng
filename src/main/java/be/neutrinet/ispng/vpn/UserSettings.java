package be.neutrinet.ispng.vpn;

import be.neutrinet.ispng.VPN;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Optional;

/**
 * Created by wannes on 8/9/14.
 */
public class UserSettings {
    private final static ObjectMapper om = new ObjectMapper();
    private final static String directory = VPN.cfg.getProperty("settings.dir");
    protected String userId;
    private HashMap<String, Object> settings;

    public UserSettings(String userId) {
        this.userId = userId;
        this.settings = new HashMap<>();

        load();
    }

    public Optional<Object> get(Object key) {
        return Optional.of(settings.get(key));
    }

    public <V> V get(Object key, V defaultValue) {
        if (!settings.containsKey(key)) return defaultValue;

        return (V) get(key);
    }

    public Object put(String key, Object value) {
        // Primitive security check to avoid disk flooding
        if (settings.size() > 1000) {
            throw new IndexOutOfBoundsException("Settings limit reached");
        }

        Object result = settings.put(key, value);

        save();

        return result;
    }

    public Object remove(String key) {
        Object result = settings.remove(key);

        save();

        return result;
    }

    public void save() {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        try {
            FileOutputStream fos = new FileOutputStream(new File(dir.getAbsolutePath() + "/" + userId + ".json"));
            om.writeValue(fos, settings);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void load() {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        try {
            File f = new File(dir.getAbsolutePath() + "/" + userId + ".json");
            if (!f.exists()) return;
            FileInputStream fos = new FileInputStream(f);
            settings = om.readValue(fos, HashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
