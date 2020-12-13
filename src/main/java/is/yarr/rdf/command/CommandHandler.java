package is.yarr.rdf.command;

import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.filler.FileFiller;
import is.yarr.rdf.filler.RandomFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, version = "RITDriveFiller v1.0.0", customSynopsis = "dunno here [-hV]")
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private final RITDriveFiller driveFiller;

    @Option(names = {"-f", "--file"}, description = "The file to upload")
    String data;

    @Option(names = {"-r", "--rname"}, description = "Random name", defaultValue = "false")
    boolean randomName;

    @Option(names = {"-p", "--parent"}, description = "The ID of the parent folder of the filled content", required = true)
    String parentId;

    @Option(names = {"-c", "--count"}, description = "The amount of times to fill with whatever strategy being used, -1 for indefinitely", defaultValue = "-1")
    int count;

    @Option(names = {"-t", "--threads"}, description = "The amount of threads to use", defaultValue = "1")
    int threads;

    @Option(names = {"-d", "--delay"}, description = "The delay in ms between each fill", defaultValue = "500")
    long delay;

    // Optimal width/height is 2000, 2000
    @Option(names = {"-w", "--width"}, description = "The width of a random image")
    int width;

    @Option(names = {"-y", "--height"}, description = "The height of a random image")
    int height;

    public CommandHandler(RITDriveFiller driveFiller) {
        this.driveFiller = driveFiller;
    }

    @Override
    public void run() {
        driveFiller.init();

        var services = driveFiller.getServices();
        var drive = services.getDrive();

        if (count <= 0) {
            count = -1;
        }

        if (delay < 0) {
            LOGGER.error("--delay must nopt be negative");
            return;
        }

        if ((width > 0 && height == 0) || (width == 0 && height > 0)) {
            LOGGER.error("If either --width or --height are above 0, the other must be as well.");
            return;
        }

        try {
            var parentFile = drive.files().get(parentId).execute();

            if (data != null) {
                var dataPath = Paths.get(data);
                if (!Files.exists(dataPath) || Files.isDirectory(dataPath)) {
                    LOGGER.error("Data file either doesn't exist or is a Directory");
                    return;
                }

                var filler = new FileFiller(parentFile, services, dataPath, randomName, threads);
                filler.fillIncrementally(count, delay);
            } else if (width != 0) {
                var filler = new RandomFiller(parentFile, services, width, height, threads);
                filler.fillIncrementally(count, delay);
            }

        } catch (IOException e) {
            LOGGER.error("There was a problem getting the parent file of ID '" + parentId + "'", e);
        }
    }
}
