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
    private final String teamDriveId;
    private final Hold hold = new Hold();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean disabled = new AtomicBoolean();

    /**
     * Invoked when {@link #waitForRateLimit()} is called
     */
    private Runnable rateLimitListener;

    public RateLimitTester(GoogleServices services, File parentFile, String teamDriveId) {
        this.services = services;
        this.parentFile = parentFile;
        this.teamDriveId = teamDriveId;
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
                                .setTeamDriveId(teamDriveId)
                                .setParents(Collections.singletonList(parentFile.getId())),
                        new ByteArrayContent("image/png", new byte[]{0}))
                        .setFields("id")
                        .setSupportsTeamDrives(true);

                request.getMediaHttpUploader()
                        .setDirectUploadEnabled(false)
                        .setChunkSize(100 * 0x100000); // 100MB (Default 10)

                try {
                    drive.files()
                            .delete(request.execute().getId())
                            .setSupportsTeamDrives(true)
                            .execute();
                } catch (GoogleJsonResponseException e) {
                    if (e.getDetails().getErrors().get(0).getDomain().equals("usageLimits")) {
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
     * Disables the rate limit tester. This only makes {@link #waitForRateLimit()} do nothing.
     */
    public void disable() {
        disabled.set(true);
    }

    /**
     * Re-enabled the rate limit tester
     */
    public void enable() {
        disabled.set(false);
    }

    /**
     * Checks if the rate limit tester is disabled. No matter what, however, in the background rate limits will be
     * tested in case it is re-enabled.
     *
     * @return If the rate limit tester is disabled
     */
    public boolean isDisabled() {
        return disabled.get();
    }

    /**
     * Waits the current thread until the program is no longer being rate limited.
     */
    public void waitForRateLimit() {
        if (rateLimitListener != null) {
            rateLimitListener.run();
        }

        if (disabled.get()) {
            return;
        }

        try {
            hold.waitForRelease();
        } catch (InterruptedException e) {
            LOGGER.error("An error occurred while waiting for rate limit release", e);
        }
    }

    /**
     * Sets a rate limit listener invoked when {@link #waitForRateLimit()} is called, even if disabled.
     *
     * @param listener The listener
     */
    public void setRateLimitListener(Runnable listener) {
        rateLimitListener = listener;
    }
}
