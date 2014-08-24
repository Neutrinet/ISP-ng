package be.neutrinet.ispng.config;

/**
 * Created by wannes on 24/08/14.
 */
public class Config {
    private final static Config instance = new Config();

    private Config() {

    }

    public Config get() {
        return instance;
    }
}
