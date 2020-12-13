package is.yarr.rdf.config;

import is.yarr.rdf.config.json.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHandler.class);

    private final Config config;

    public ConfigHandler(Config config) {
        this.config = config;
    }

    /**
     * Starts the filling of files.
     */
    public void beginFill(boolean sequential) throws InterruptedException {

    }
}
