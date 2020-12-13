package is.yarr.rdf.command;

import com.google.gson.Gson;
import is.yarr.rdf.RITDriveFiller;
import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.config.ConfigHandler;
import is.yarr.rdf.config.json.Config;
import is.yarr.rdf.config.json.UserData;
import is.yarr.rdf.filler.strategies.FillStrategy;
import is.yarr.rdf.filler.strategies.FillStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;

@CommandLine.Command(name = "example", mixinStandardHelpOptions = true, version = "RITDriveFiller v1.0.0", customSynopsis = "dunno here [-hV]")
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

    @Option(names = {"-g", "--generate"}, description = "If it should only generate a token in --token")
    boolean generate;

    @Option(names = {"-q", "--sequential"}, description = "If multiple users in the config should upload files one at a time")
    boolean sequential;

    @Option(names = {"-a", "--account"}, description = "If using a single account, the name of it in the config")
    String accountName;

    @Option(names = {"-o", "--config"}, description = "A file to a JSON config, overriding user-specific arguments")
    String configFile;

    @Option(names = {"-s", "--token"}, description = "The directory to store user token", defaultValue = "default")
    String tokenDir;

    @Option(names = {"-i", "--teamdrive"}, description = "If using a team drive, its ID")
    String teamDriveId;

    @Option(names = {"-f", "--file"}, description = "The file to upload")
    String data;

    @Option(names = {"-r", "--rname"}, description = "Random name", defaultValue = "false")
    boolean randomName;

    @Option(names = {"-p", "--parent"}, description = "The ID of the parent folder of the filled content")
    String parentId;

    @Option(names = {"-c", "--count"}, description = "The amount of times to fill with whatever strategy being used, -1 for indefinitely", defaultValue = "-1")
    int count;

    @Option(names = {"-t", "--threads"}, description = "The amount of threads to use", defaultValue = "1")
    int threads;

    @Option(names = {"-d", "--delay"}, description = "The delay in ms between each fill", defaultValue = "500")
    long delay;

    @Override
    public void run() {
        if (generate) {
            if (tokenDir == null) {
                LOGGER.error("--token can't be null");
                return;
            }

            RITDriveFiller.createServices("Generator", tokenDir);

            LOGGER.info("Created token in {}", tokenDir);
            return;
        }

        if (count <= 0) {
            count = -1;
        }

        if (delay < 0) {
            LOGGER.error("--delay must not be negative");
            return;
        }


        try {
            var config = generateConfig();

            var fillStrategyFactory = new FillStrategyFactory();
            var fillStrategy = fillStrategyFactory.createFillStrategy(config, sequential);
            fillStrategy.beginFill(accountName);
        } catch (InterruptedException | FileNotFoundException e) {
            LOGGER.error("An error occurred while filling data", e);
        }
    }

    private Config generateConfig() throws FileNotFoundException {
        if (configFile != null) {
            return new Gson().fromJson(new FileReader(configFile), Config.class);
        }

        return new Config().setUsers(Collections.singletonList(new UserData()
                .setName("Default")
                .setTokenPath(tokenDir)
                .setUploadTo(parentId)
                .setTeamDrive(teamDriveId)
                .setFile(data)
                .setRandomName(randomName)
        ));
    }
}
