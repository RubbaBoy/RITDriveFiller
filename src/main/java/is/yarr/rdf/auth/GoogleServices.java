package is.yarr.rdf.auth;

import com.google.api.services.drive.Drive;

import javax.annotation.Nonnull;

/**
 * An object to store all services by Google.
 */
public class GoogleServices {

    private final Drive drive;

    public GoogleServices(@Nonnull Drive drive) {
        this.drive = drive;
    }

    /**
     * Gets the created {@link Drive}
     * @return The {@link Drive}
     */
    public @Nonnull Drive getDrive() {
        return drive;
    }
}
