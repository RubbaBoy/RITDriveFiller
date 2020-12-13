package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.RollingAverageManager;
import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.config.json.Config;
import is.yarr.rdf.config.json.UserData;
import is.yarr.rdf.filler.FileFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class FillStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FillStrategy.class);

    final Config config;

    protected FillStrategy(Config config) {
        this.config = config;
    }

    Optional<FileFiller> create(UserData user, GoogleServices services, RollingAverageManager rollingAverageManager) throws IOException {
        var dataPath = Paths.get(user.getFile());

        if (!Files.exists(dataPath) || Files.isDirectory(dataPath)) {
            LOGGER.error("Data file either doesn't exist or is a Directory");
            return Optional.empty();
        }

        var parentFile = services.getDrive().files()
                .get(user.getUploadTo())
                .setSupportsTeamDrives(true)
                .execute();

        return Optional.of(new FileFiller(user.getName(), parentFile, user.getTeamDrive(), services, rollingAverageManager, dataPath, user.getRandomName(), user.getThreads()));
    }

    public abstract void beginFill(String accountName) throws InterruptedException;

}
