package is.yarr.rdf.filler;

import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveFiller.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean fresh = new AtomicBoolean(true);
    private final AtomicBoolean completed = new AtomicBoolean();

    final File parentFile;
    final GoogleServices services;

    /**
     * Creates a {@link DriveFiller}.
     *
     * @param parentId The folder ID of where the filling should be contained in
     * @param services The {@link GoogleServices}
     */
    protected DriveFiller(File parentId, GoogleServices services) {
        this.parentFile = parentId;
        this.services = services;
    }

    /**
     * Fills with {@link #fill()} indefinitely as fast as possible.
     */
    public void fillIndefinitely() {
        fillIndefinitely(0);
    }

    /**
     * Fills with {@link #fill()} indefinitely with a delay between each iteration.
     *
     * @param delayMillis The delay between each fill in milliseconds
     */
    public void fillIndefinitely(long delayMillis) {
        fillIncrementally(-1, delayMillis);
    }

    /**
     * Fills with {@link #fill()} {@param count} times as fast as possible.
     *
     * @param count The amount of times to invoke {@link #fill()}
     */
    public void fillIncrementally(int count) {
        fillIncrementally(count, 0);
    }

    /**
     * Fills with {@link #fill()} {@param count} times with a delay between each iteration.
     *
     * @param count       The amount of times to invoke {@link #fill()}
     * @param delayMillis The delay between each fill in milliseconds
     */
    public void fillIncrementally(int count, long delayMillis) {
        if (!fresh.get() && !completed.get()) {
            LOGGER.error("DriveFiller busy!");
            return;
        }

        completed.set(false);
        fresh.set(false);
        var activeTask = scheduledExecutorService.scheduleAtFixedRate(() -> {
            var iterations = count == -1 ? Integer.MAX_VALUE : count;
            for (int i = 0; i < iterations; i++) {
                if (!fill()) {
                    break;
                }
            }

            completed.set(true);
        }, 0, delayMillis, TimeUnit.MILLISECONDS);

        try {
            while (!completed.get()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {} finally {
            activeTask.cancel(true);
        }
    }

    /**
     * Fills with whatever implemented method once.
     *
     * @return If the fill was successful. If false, any multiple fill method should stop.
     */
    abstract boolean fill();

    public GoogleServices getServices() {
        return services;
    }
}
