package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.RollingAverageManager;
import is.yarr.rdf.config.json.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Once a single account hits xxxMB uploaded, it moves to a new account.
 */
public class AccountRolloverStrategy extends FillStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRolloverStrategy.class);

    // The upload limit in BYTES
    private final long uploadLimit;

    /**
     * Creates an AccountRolloverStrategy with a config and upload limit.
     *
     * @param config      The config
     * @param uploadLimit The amount of MB that can be uploaded before moving to a new account
     */
    protected AccountRolloverStrategy(Config config, int uploadLimit) {
        super(config);
        this.uploadLimit = uploadLimit * 1_000_000L;
    }

    @Override
    public void beginFill(String accountName) {
        LOGGER.info("Filling in account rollover mode");

        var count = config.getUsers().size();
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

                var filler = fillerOptional.get();
                var fileCount = (int) (uploadLimit / (long) filler.getFileSize());
                LOGGER.info("Uploading {} files before moving onto the next account", fileCount);

                var rateLimitTester = filler.getRateLimitTester();
                rateLimitTester.disable();

                var latchOptional = filler.fillIncrementally(fileCount, user.getDelay());

                if (latchOptional.isEmpty()) {
                    continue;
                }

                var latch = latchOptional.get();

                var printed = new AtomicBoolean();
                rateLimitTester.setRateLimitListener(() -> {
                    if (!printed.getAndSet(true)) {
                        LOGGER.info("Account {} has been rate limited, releasing its lock and continuing", user.getName());
                    }

                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    LOGGER.error("An error occurred while waiting for incremental latch", e);
                }

                LOGGER.info("Lock has been released, moving onto a new account");
            } catch (IOException e) {
                LOGGER.error("An error occurred while filling account '" + user.getName() + "'", e);
            }
        }

        LOGGER.info("Used up all accounts");
    }
}
