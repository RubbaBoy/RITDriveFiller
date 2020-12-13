package is.yarr.rdf;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.concurrent.Hold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RateLimitTester {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitTester.class);

    private final GoogleServices services;
    private final File parentFile;
    private final Hold hold = new Hold();
    private final AtomicBoolean started = new AtomicBoolean();

    public RateLimitTester(GoogleServices services, File parentFile) {
        this.services = services;
        this.parentFile = parentFile;
    }

    /**
     * Starts checking for rate limits every 30 minutes. If already started, nothing will happen.
     */
    public void startChecking() {
        if (started.getAndSet(true)) {
            return;
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (hold.getWaitingCount() == 0) {
                return;
            }

            try {
                var drive = services.getDrive();

                var request = drive.files().create(new File()
                                .setName("temp")
                                .setParents(Collections.singletonList(parentFile.getId())),
                        new ByteArrayContent("image/png", new byte[]{0})).setFields("id");

                request.getMediaHttpUploader()
                        .setDirectUploadEnabled(false)
                        .setChunkSize(100 * 0x100000); // 100MB (Default 10)

                try {
                    drive.files().delete(request.execute().getId()).execute();
                } catch (GoogleJsonResponseException e) {
                    if (e.getDetails().getMessage().equals("User rate limit exceeded.")) {
                        LOGGER.debug("Hit rate limit!");
                    } else {
                        LOGGER.error("Hit other error during rate limit checking", e);
                    }
                    return;
                }

                LOGGER.debug("No longer rate limited!");

                hold.release();
            } catch (IOException e) {
                LOGGER.error("A general error occurred during rate limit checking", e);
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    /**
     * Waits the current thread until the program is no longer being rate limited.
     */
    public void waitForRateLimit() {
        try {
            hold.waitForRelease();
        } catch (InterruptedException e) {
            LOGGER.error("An error occurred while waiting for rate limit release", e);
        }
    }
}
