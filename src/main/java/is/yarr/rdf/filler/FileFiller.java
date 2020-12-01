package is.yarr.rdf.filler;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class FileFiller extends DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFiller.class);

    private final Path dataPath;

    public FileFiller(GoogleServices services, File parentFile, Path dataPath) {
        super(parentFile, services);
        this.dataPath = dataPath;
    }

    @Override
    boolean fill() {
        var drive = services.getDrive();

        try {
            var content = new ByteArrayContent(Files.probeContentType(dataPath), Files.readAllBytes(dataPath));
            var request = drive.files().create(new File()
                    .setName(dataPath.toFile().getName())
                    .setParents(Collections.singletonList(parentFile.getId())
                    ), content).setFields("");

            request.getMediaHttpUploader()
                    .setDirectUploadEnabled(false)
                    .setChunkSize(100 * 0x100000); // 100MB (Default 10)

            var start = System.currentTimeMillis();
            request.execute();
            LOGGER.info("Uploaded in {}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.error("An exception occurred while uploading data file '" + dataPath + "'", e);
            return false;
        }

        return true;
    }
}
