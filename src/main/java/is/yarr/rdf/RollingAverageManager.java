package is.yarr.rdf;

import com.google.common.util.concurrent.AtomicDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RollingAverageManager {

    private static final int EVERY_SECONDS = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingAverageManager.class);
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.00");

    private final AtomicDouble bytesUploaded = new AtomicDouble();

    private final RollingAverage rollingAverage;

    private boolean initial = true;

    public RollingAverageManager(int dataPoints) {
        this.rollingAverage = new RollingAverage(dataPoints);
    }

    /**
     * Starts the printing of data.
     */
    public void startDisplay() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var bytes = bytesUploaded.getAndSet(0);
            if (bytes == 0) {
                if (initial) {
                    return;
                }
            } else if (initial) {
                initial = false;
            }

            var mbs = (bytes / 1_000_000D) / (double) EVERY_SECONDS;
            rollingAverage.add(mbs);
            LOGGER.info("Speed: {} mb/s  (20 ma: {} mb/s)", FORMATTER.format(mbs), FORMATTER.format(rollingAverage.getAverage()));
        }, 0, EVERY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Adds bytes to the total amount uploaded to calculate speed.
     *
     * @param bytes The amount of bytes added
     */
    public void addBytes(double bytes) {
        bytesUploaded.addAndGet(bytes);
    }
}
