package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.config.json.Config;

public class FillStrategyFactory {

    public FillStrategy createFillStrategy(Config config, boolean sequential) {
        if (sequential) {
            return new SequentialStrategy(config);
        } else {
            return new ParallelStrategy(config);
        }
    }

}
