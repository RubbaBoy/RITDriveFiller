package is.yarr.rdf.config;

import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.RollingAverageManager;
import is.yarr.rdf.config.json.Config;
import is.yarr.rdf.filler.FileFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

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
