package is.yarr.rdf;

import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.auth.LocalGoogleServiceCreator;
import is.yarr.rdf.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class RITDriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RITDriveFiller.class);

    private GoogleServices services;

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"-h"};
        }

        System.exit(new CommandLine(new CommandHandler(new RITDriveFiller())).execute(args));
    }

    public void init() {
        var authMan = new LocalGoogleServiceCreator();

        var servicesOptional = authMan.createServices("credentials.json");
        if (servicesOptional.isEmpty()) {
            LOGGER.error("Unable to create GoogleServices");
            System.exit(0);
        }

        services = servicesOptional.get();
    }

    public GoogleServices getServices() {
        return services;
    }

}
