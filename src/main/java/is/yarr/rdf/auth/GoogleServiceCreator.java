package is.yarr.rdf.auth;

import java.util.Optional;

/**
 * Creates an instnce of {@link GoogleServices}.
 */
public interface GoogleServiceCreator {

    /**
     * Creates a {@link GoogleServices} from a given app credential path.
     * @param credentialPath Either a path to the JSON credentials file, the raw JSON contents, or an environment
     *                       variable name
     * @param tokenDirectory The directory to store individual users' token data
     * @return A {@link GoogleServices} instance, if no errors occurred
     */
    Optional<GoogleServices> createServices(String credentialPath, String tokenDirectory);
}
