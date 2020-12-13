package is.yarr.rdf.filler;

import com.google.api.services.drive.model.File;
import com.google.common.util.concurrent.AtomicDouble;
import is.yarr.rdf.RollingAverage;
import is.yarr.rdf.auth.GoogleServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;

public class FileFiller extends DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFiller.class);
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.00");

    private final Path dataPath;
    private final boolean randomName;

    private final RollingAverage average = new RollingAverage(20);
    private final AtomicDouble bytesUploaded = new AtomicDouble();
    private byte[] data;

    private boolean initial = true;

    public FileFiller(File parentFile, String teamDriveId, GoogleServices services, Path dataPath, boolean randomName, int threads) {
        super(parentFile, teamDriveId, services, threads);
        this.dataPath = dataPath;
        this.randomName = randomName;

        try {
            this.data = Files.readAllBytes(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var bytes = bytesUploaded.getAndSet(0);
            if (bytes == 0) {
                if (initial) {
                    return;
                }
            } else if (initial) {
                initial = false;
            }

            var mbs = (bytes / 1_000_000D) / 3D;
            average.add(mbs);
            LOGGER.info("Speed: {} mb/s  (20 ma: {} mb/s)", FORMATTER.format(mbs), FORMATTER.format(average.getAverage()));
        }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    @Override
    boolean fill() {
        try {
            var name = randomName ? generateName(12) : dataPath.toFile().getName();
            var start = System.currentTimeMillis();
            uploadData(name, Files.probeContentType(dataPath), data);
            var time = System.currentTimeMillis() - start;
            bytesUploaded.addAndGet(Files.size(dataPath));
            LOGGER.info("Uploaded in {}ms", time);
        } catch (IOException e) {
            LOGGER.error("An exception occurred while uploading data file '" + dataPath + "'", e);
            return false;
        }

        return true;
    }
}
