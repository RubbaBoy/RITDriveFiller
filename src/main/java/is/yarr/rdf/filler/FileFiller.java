package is.yarr.rdf.filler;

import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileFiller extends DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFiller.class);

    private final Path dataPath;
    private final boolean randomName;

    public FileFiller(File parentFile, GoogleServices services, Path dataPath, boolean randomName) {
        super(parentFile, services);
        this.dataPath = dataPath;
        this.randomName = randomName;
    }

    @Override
    boolean fill() {
        try {
            var name = randomName ? generateName(12) : dataPath.toFile().getName();
            var start = System.currentTimeMillis();
            uploadData(name, Files.probeContentType(dataPath), Files.readAllBytes(dataPath));
            LOGGER.info("Uploaded in {}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.error("An exception occurred while uploading data file '" + dataPath + "'", e);
            return false;
        }

        return true;
    }
}
