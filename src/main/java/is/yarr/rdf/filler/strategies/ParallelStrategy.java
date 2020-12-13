package is.yarr.rdf.filler.strategies;

import com.google.api.services.drive.Drive;
import is.yarr.rdf.RITDriveFiller;
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
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class ParallelStrategy extends FillStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelStrategy.class);

    protected ParallelStrategy(Config config) {
        super(config);
    }

    @Override
    public void beginFill(String accountName) throws InterruptedException {
        LOGGER.info("Filling in parallel mode");

        var count = config.getUsers().size();
        var latches = new ArrayList<CountDownLatch>();
        var rollingAverageManager = new RollingAverageManager(10);
        rollingAverageManager.startDisplay();

        for (int i = 0; i < count; i++) {
            var user = config.getUsers().get(i);

            if (accountName != null && !user.getName().equals(accountName)) {
                continue;
            }

            var services = RITDriveFiller.createServices(user.getName(), user.getTokenPath());

            try {
                LOGGER.info("Starting user {}", user.getName());

                var fillerOptional = create(user, services, rollingAverageManager);
                if (fillerOptional.isEmpty()) {
                    continue;
                }

                fillerOptional.get().fillIncrementally(user.getCount(), user.getDelay()).ifPresent(latches::add);

                Thread.sleep(5000);
            } catch (IOException e) {
                LOGGER.error("An error occurred while filling account '" + user.getName() + "'", e);
            }
        }

        for (var latch : latches) {
            latch.await();
        }
    }
}
