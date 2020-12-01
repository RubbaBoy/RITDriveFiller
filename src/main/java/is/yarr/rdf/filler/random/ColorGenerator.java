package is.yarr.rdf.filler.random;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class ColorGenerator {

    private final float[] hsbValues = new float[3];

    /**
     * Creates a ColorGenerator with a random base color.
     */
    public ColorGenerator() {
        this(new Color(ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat(), ThreadLocalRandom.current().nextFloat()));
    }

    /**
     * Creates a ColorGenerator with a base color.
     *
     * @param baseColor The base color
     */
    public ColorGenerator(Color baseColor) {
        Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), hsbValues);
    }

    /**
     * Generates a color with varied hue with given variance.
     *
     * @return The new color
     */
    public Color generateColor() {
        return new Color(Color.HSBtoRGB(ThreadLocalRandom.current().nextFloat(), hsbValues[1], hsbValues[2]));
    }
}
