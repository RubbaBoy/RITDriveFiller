package is.yarr.rdf.filler;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * Uploads binary data to Google Drive, using the provided parent ID.
     *
     * @param name The name of the file
     * @param mimeType The real MIME type of the file
     * @param bytes The byte content of the file
     * @return The ID of the file
     * @throws IOException If an exception occurs
     */
    String uploadData(String name, String mimeType, byte[] bytes) throws IOException {
        var drive = services.getDrive();

        var content = new ByteArrayContent(mimeType, bytes);
        var request = drive.files().create(new File()
                .setName(name)
                .setParents(Collections.singletonList(parentFile.getId())),
                content).setFields("id");

        request.getMediaHttpUploader()
                .setDirectUploadEnabled(false)
                .setChunkSize(100 * 0x100000); // 100MB (Default 10)

        return request.execute().getId();
    }

    /**
     * Generates a random alphanumeric name (_ and - permitted as well) with a given length.
     *
     * @param length The length of the name
     * @return The name
     */
    String generateName(int length) {
        var values = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
        return IntStream.range(0, length)
                .mapToObj($ -> values.charAt(ThreadLocalRandom.current().nextInt(0, values.length() - 1)))
                .map(String::valueOf)
                .collect(Collectors.joining());
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
