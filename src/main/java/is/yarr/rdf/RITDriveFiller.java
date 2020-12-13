package is.yarr.rdf;

import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.auth.LocalGoogleServiceCreator;
import is.yarr.rdf.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class RITDriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RITDriveFiller.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"-h"};
        }

        System.exit(new CommandLine(new CommandHandler()).execute(args));
    }

    public static GoogleServices createServices(String name, String tokenDirectory) {
        var authMan = new LocalGoogleServiceCreator();

        var servicesOptional = authMan.createServices("credentials.json", tokenDirectory);
        if (servicesOptional.isEmpty()) {
            LOGGER.error("Unable to create GoogleServices for {} in directory {}", name, tokenDirectory);
            System.exit(0);
        }

        return servicesOptional.get();
    }

}
