package is.yarr.rdf.filler.strategies;

import is.yarr.rdf.config.json.Config;

public class FillStrategyFactory {

    public FillStrategy createFillStrategy(Config config, int uploadLimit, boolean sequential) {
        if (uploadLimit != -1) {
            return new AccountRolloverStrategy(config, uploadLimit);
        } else if (sequential) {
            return new SequentialStrategy(config);
        } else {
            return new ParallelStrategy(config);
        }
    }

}
