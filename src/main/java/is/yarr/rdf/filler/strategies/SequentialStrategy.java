package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.RollingAverageManager;
import is.yarr.rdf.config.json.Config;
import is.yarr.rdf.filler.FileFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SequentialStrategy extends FillStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialStrategy.class);

    protected SequentialStrategy(Config config) {
        super(config);
    }

    @Override
    public void beginFill(String accountName) throws InterruptedException {
        LOGGER.info("Filling in sequential mode");

        var count = config.getUsers().size();
        var fillers = new HashMap<String, FileFiller>();
        var rollingAverageManager = new RollingAverageManager(10);
        rollingAverageManager.startDisplay();

        int threads = 0;

        for (int i = 0; i < count; i++) {
            var user = config.getUsers().get(i);

            if (accountName != null && !user.getName().equals(accountName)) {
                continue;
            }

            var services = RITDriveFiller.createServices(user.getName(), user.getTokenPath());

            try {
                if (user.getName().equals("aty3425")) {
                    continue;
                }

                LOGGER.info("Starting on user {}", user.getName());

                var fillerOptional = create(user, services, rollingAverageManager);
                if (fillerOptional.isEmpty()) {
                    continue;
                }
                var filler = fillerOptional.get();

                filler.getRateLimitTester().disable();

                threads = Math.max(threads, user.getThreads());

                fillers.put(user.getName(), filler);
            } catch (IOException e) {
                LOGGER.error("An error occurred while filling account '" + user.getName() + "'", e);
            }
        }

        LOGGER.info("Starting with {} threads", threads);
        var executor = Executors.newFixedThreadPool(threads);

        for (int i1 = 0; i1 < threads; i1++) {
            int finalI = i1;
            executor.execute(() -> {
                while (true) {
                    for (var name : fillers.keySet()) {
                        var filler = fillers.get(name);
                        LOGGER.info("[{}] Filling #{}", name, finalI);
                        filler.fill();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1000, TimeUnit.DAYS);
    }
}
