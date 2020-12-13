package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.config.json.Config;

public abstract class FillStrategy {

    final Config config;

    protected FillStrategy(Config config) {
        this.config = config;
    }

    public abstract void beginFill(String accountName) throws InterruptedException;

}
