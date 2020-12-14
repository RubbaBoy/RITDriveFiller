package is.yarr.rdf;

import java.util.ArrayList;
import java.util.List;

public class RollingAverage {

    private final int length;
    private final List<Double> values = new ArrayList<>();

    public RollingAverage(int length) {
        this.length = length;
    }

    public void add(double value) {
        values.add(value);
        if (values.size() > length) {
            values.remove(0);
        }
    }

    public double getAverage() {
        var total = 0.0D;
        for (var value : values) {
            total += value;
        }
        return total / values.size();
    }
}
