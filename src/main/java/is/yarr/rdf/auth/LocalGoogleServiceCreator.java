package is.yarr.rdf.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import is.yarr.rdf.http.BackOffInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class LocalGoogleServiceCreator implements GoogleServiceCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalGoogleServiceCreator.class);

    private static final String APPLICATION_NAME = "RITDriveFiller";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "private";
    private static final List<String> SCOPES = List.of(DriveScopes.DRIVE);

    @Override
    public Optional<GoogleServices> createServices(String credentialPath) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            var credentials = getCredentials(credentialPath, HTTP_TRANSPORT);

            if (credentials == null) {
                return Optional.empty();
            }

            var drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                    .setApplicationName(APPLICATION_NAME)
                    .setHttpRequestInitializer(new BackOffInitializer(credentials))
                    .build();

            return Optional.of(new GoogleServices(drive));
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("An error occurred while creating GoogleServices", e);
            return Optional.empty();
        }
    }

    private Credential getCredentials(String credentialPath, NetHttpTransport HTTP_TRANSPORT) throws IOException {
        var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, credentialsReader(credentialPath));

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Reader credentialsReader(String creds) throws FileNotFoundException {
        if (creds.contains("{")) {
            LOGGER.info("Using credentials from direct JSON");
            return new StringReader(creds);
        }

        if (!creds.contains(".json")) {
            LOGGER.info("Using credentials from environment variable \"{}\"", creds);
            return new StringReader(System.getenv(creds));
        }

        var file = new File(creds);
        LOGGER.info("Using credentials from file \"{}\"", file.getAbsolutePath());
        if (!file.exists()) {
            throw new FileNotFoundException("Couldn't find credentials file " + creds);
        }

        return new FileReader(file);
    }

}
