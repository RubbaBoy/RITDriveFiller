package is.yarr.rdf.command;

import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.filler.FileFiller;
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

    @Option(names = {"-f", "--file"}, description = "The file to upload", required = true)
    String data;

    @Option(names = {"-p", "--parent"}, description = "The ID of the parent folder of the filled content", required = true)
    String parentId;

    @Option(names = {"-c", "--count"}, description = "The amount of times to fill with whatever strategy being used, -1 for indefinitely", defaultValue = "-1")
    int count;

    @Option(names = {"-d", "--delay"}, description = "The delay in ms between each fill", defaultValue = "500")
    long delay;

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

        try {
            var parentFile = drive.files().get(parentId).execute();

            if (data != null) {
                var dataPath = Paths.get(data);
                if (!Files.exists(dataPath) || Files.isDirectory(dataPath)) {
                    LOGGER.error("Data file either doesn't exist or is a Directory");
                    return;
                }

                var filler = new FileFiller(services, parentFile, dataPath);
                filler.fillIncrementally(count, delay);
            }

        } catch (IOException e) {
            LOGGER.error("There was a problem getting the parent file of ID '" + parentId + "'", e);
        }
    }
}
