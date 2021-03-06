package is.yarr.rdf.filler;

import com.google.api.services.drive.model.File;
import is.yarr.rdf.RollingAverageManager;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileFiller extends DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFiller.class);

    private final String name;
    private final RollingAverageManager rollingAverageManager;
    private final Path dataPath;
    private final boolean randomName;

    private static byte[] data;

    public FileFiller(String name, File parentFile, String teamDriveId, GoogleServices services, RollingAverageManager rollingAverageManager, Path dataPath, boolean randomName, int threads) {
        super(parentFile, teamDriveId, services, threads);
        this.name = name;
        this.rollingAverageManager = rollingAverageManager;
        this.dataPath = dataPath;
        this.randomName = randomName;

        try {
            if (data == null) {
                data = Files.readAllBytes(dataPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean fill() {
        try {
            var name = randomName ? generateName(12) : dataPath.toFile().getName();
            var start = System.currentTimeMillis();
            var optional = uploadData(name, Files.probeContentType(dataPath), data);

            if (optional.isEmpty() && getRateLimitTester().isDisabled()) {
                return false;
            }

            var time = System.currentTimeMillis() - start;
            rollingAverageManager.addBytes(data.length);
            LOGGER.info("[{}] Uploaded in {}ms", this.name, time);
        } catch (IOException e) {
            LOGGER.error("[" + this.name + "] An exception occurred while uploading data file '" + dataPath + "'", e);
            return false;
        }

        return true;
    }

    public int getFileSize() {
        return data.length;
    }
}
