package is.yarr.rdf;

import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import is.yarr.rdf.config.json.Config;
import is.yarr.rdf.config.json.UserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class FileMover {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileMover.class);

    private final UserData user;

    private final JsonBatchCallback<File> callback = new JsonBatchCallback<>() {
        @Override
        public void onFailure(GoogleJsonError e,
                              HttpHeaders responseHeaders) {
            LOGGER.error("An error occurred during batch request: {}", e.getMessage());
        }

        @Override
        public void onSuccess(File file,
                              HttpHeaders responseHeaders) {}
    };

    public FileMover(Config config, String accountName) {
        this.user = config.getUsers().stream().filter(user -> user.getName().equals(accountName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No account found with the name " + accountName));
    }

    public void moveChildren(String parentId) throws IOException {
        var uploadTo = user.getUploadTo();

        var services = RITDriveFiller.createServices(user.getName(), user.getTokenPath());
        var drive = services.getDrive();

        LOGGER.info("Here {} -> {}", parentId, uploadTo);

        var ayarris = drive.files().get("1i7N3nvJfsHnUniPxHJyxYGBygrBdfUTJ")
                .setSupportsTeamDrives(true)
                .setFields("id, parents")
                .execute();

        LOGGER.info("Parents: {}", ayarris.getParents());

        drive.files()
                .update("1i7N3nvJfsHnUniPxHJyxYGBygrBdfUTJ", new File())
                .setSupportsTeamDrives(true)
                .setAddParents("1b9xj05MXD6zmu4YHR3GWTVSJYybowzDS")
                .setFields("id, parents")
                .execute();

        if (true) return;

        int count = 0;
        var pageToken = "";
        var batch = drive.batch();
        do {
            var result = listPaged(pageToken, drive, parentId);
            pageToken = result.getNextPageToken();
            var files = result.getFiles();
            if (files == null || files.isEmpty()) {
                break;
            }

            for (var file : files) {
                drive.files()
                        .update(file.getId(), new File())
                        .setSupportsTeamDrives(true)
                        .setAddParents(uploadTo)
                        .setRemoveParents(parentId)
                        .setFields("id, parents")
                        .queue(batch, callback);
                count++;

                if (count == 50) {
                    LOGGER.info("Executing batch");
                    batch.execute();
                    LOGGER.info("Executed");
                    batch = drive.batch();
                }
            }
        } while (pageToken != null);

        if (count > 0) {
            LOGGER.info("Executing last batch with {}", count);
            batch.execute();
        }
    }

    private FileList listPaged(String pageToken, Drive drive, String parentId) throws IOException {
        var builder = drive.files()
                .list()
                .setQ("parents in '" + parentId + "'")
                .setIncludeTeamDriveItems(true)
                .setSupportsTeamDrives(true)
                .setCorpora("allDrives")
                .setFields("nextPageToken, files(id, name)");

        if (pageToken != null && !pageToken.isBlank()) {
            builder.setPageToken(pageToken);
        }

        return builder.execute();
    }
}
