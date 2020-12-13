package is.yarr.rdf.filler;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import is.yarr.rdf.RateLimitTester;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriveFiller.class);

    private final AtomicBoolean fresh = new AtomicBoolean(true);
    private final CountDownLatch latch;
    private final ExecutorService executorService;
    private final RateLimitTester rateLimitTester;

    final File parentFile;
    private final String teamDriveId;
    final GoogleServices services;
    private final int threads;

    /**
     * Creates a {@link DriveFiller}.
     *
     * @param parentFile The folder of where the filling should be contained in
     * @param services The {@link GoogleServices}
     * @param threads The amount of threads to use
     */
    protected DriveFiller(File parentFile, String teamDriveId, GoogleServices services, int threads) {
        this.parentFile = parentFile;
        this.teamDriveId = teamDriveId;
        this.services = services;
        this.threads = threads;
        this.latch = new CountDownLatch(threads);
        this.executorService = Executors.newFixedThreadPool(threads);
        this.rateLimitTester = new RateLimitTester(services, parentFile, teamDriveId);
    }

    /**
     * Fills with {@link #fill()} indefinitely as fast as possible.
     *
     * @return The {@link CountDownLatch} which should be awaited for completion of the threads
     */
    public Optional<CountDownLatch> fillIndefinitely() {
        return fillIndefinitely(0);
    }

    /**
     * Fills with {@link #fill()} indefinitely with a delay between each iteration.
     *
     * @param delayMillis The delay between each fill in milliseconds
     * @return The {@link CountDownLatch} which should be awaited for completion of the threads
     */
    public Optional<CountDownLatch> fillIndefinitely(long delayMillis) {
        return fillIncrementally(-1, delayMillis);
    }

    /**
     * Fills with {@link #fill()} {@param count} times as fast as possible.
     *
     * @param count The amount of times to invoke {@link #fill()}
     * @return The {@link CountDownLatch} which should be awaited for completion of the threads
     */
    public Optional<CountDownLatch> fillIncrementally(int count) {
        return fillIncrementally(count, 0);
    }

    /**
     * Fills with {@link #fill()} {@param count} times with a delay between each iteration.
     *
     * @param count       The amount of times to invoke {@link #fill()}
     * @param delayMillis The delay between each fill in milliseconds
     * @return The {@link CountDownLatch} which should be awaited for completion of the threads
     */
    public Optional<CountDownLatch> fillIncrementally(int count, long delayMillis) {
        if (!fresh.get() && latch.getCount() != 0) {
            LOGGER.error("DriveFiller busy!");
            return Optional.empty();
        }

        rateLimitTester.startChecking();

        fresh.set(false);

        for (int i1 = 0; i1 < threads; i1++) {
            executorService.execute(() -> {
                var iterations = count == -1 ? Integer.MAX_VALUE : count;
                for (int i = 0; i < iterations; i++) {
                    if (!fill()) {
                        break;
                    }

                    if (delayMillis != 0) {
                        try {
                            Thread.sleep(delayMillis);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

                latch.countDown();
            });
        }

        return Optional.of(latch);
    }

    /**
     * Uploads binary data to Google Drive, using the provided parent ID.
     *
     * @param name The name of the file
     * @param mimeType The real MIME type of the file
     * @param file The file
     * @return The ID of the file
     * @throws IOException If an exception occurs
     */
    Optional<String> uploadData(String name, String mimeType, java.io.File file) throws IOException {
        return uploadData(name, new FileContent(mimeType, file));
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
    Optional<String> uploadData(String name, String mimeType, byte[] bytes) throws IOException {
        return uploadData(name, new ByteArrayContent(mimeType, bytes));
    }

    Optional<String> uploadData(String name, AbstractInputStreamContent content) throws IOException {
        var drive = services.getDrive();

        var request = drive.files().create(new File()
                .setName(name)
                .setTeamDriveId(teamDriveId)
                .setParents(Collections.singletonList(parentFile.getId())),
                content)
                .setFields("id")
                .setSupportsTeamDrives(true);

        request.getMediaHttpUploader()
                .setDirectUploadEnabled(false)
                .setChunkSize(100 * 0x100000); // 100MB (Default 10)

        try {
            return Optional.of(request.execute().getId());
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().getMessage().contains("rate limit")) {
                LOGGER.debug("Hit rate limit!");
                rateLimitTester.waitForRateLimit();
            } else {
                LOGGER.error("Unknown error while uploading data", e);
            }
        }

        return Optional.empty();
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
