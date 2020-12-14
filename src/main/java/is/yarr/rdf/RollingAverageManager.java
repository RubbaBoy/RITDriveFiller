package is.yarr.rdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static is.yarr.rdf.Utility.humanReadableByteCountSI;

public class RollingAverageManager {

    private static final long EVERY_SECONDS = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingAverageManager.class);

    private final AtomicLong totalBytesUploaded = new AtomicLong();
    private final AtomicLong bytesUploaded = new AtomicLong();

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

            var mbs = bytes / EVERY_SECONDS;
            rollingAverage.add(mbs);
            LOGGER.info("Speed: {}/s  (20 ma: {}/s)  Total: {}", humanReadableByteCountSI(mbs), humanReadableByteCountSI((long) rollingAverage.getAverage()), humanReadableByteCountSI(totalBytesUploaded.get()));
        }, 0, EVERY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Adds bytes to the total amount uploaded to calculate speed.
     *
     * @param bytes The amount of bytes added
     */
    public void addBytes(long bytes) {
        bytesUploaded.addAndGet(bytes);
        totalBytesUploaded.addAndGet(bytes);
    }
}
