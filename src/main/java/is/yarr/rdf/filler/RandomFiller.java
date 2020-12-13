package is.yarr.rdf.filler;

import com.google.api.services.drive.model.File;
import is.yarr.rdf.auth.GoogleServices;
import is.yarr.rdf.filler.random.ColorGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RandomFiller extends DriveFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomFiller.class);
    private final int width;
    private final int height;


    /**
     * Creates a {@link DriveFiller}.
     *  @param parentId The folder ID of where the filling should be contained in
     * @param services The {@link GoogleServices}
     * @param width The width of the generated image
     * @param height The height of the generated image
     * @param threads
     */
    public RandomFiller(File parentId, GoogleServices services, int width, int height, int threads) {
        super(parentId, services, threads);
        this.width = width;
        this.height = height;
    }

    @Override
    boolean fill() {
        var name = generateName(10);

        try {

            var image = generateRandomBytes(width, height);

            var start = System.currentTimeMillis();
            uploadData(name, "image/png", image);
            LOGGER.info("Uploaded in {}ms", System.currentTimeMillis() - start);
        } catch (IOException e) {
            LOGGER.error("An exception occurred while uploading binary content named '" + name + "'", e);
            return false;
        }

        return true;
    }

    private byte[] generateRandomBytes(int width, int height) throws IOException {
        var colorGenerator = new ColorGenerator();
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, colorGenerator.generateColor().getRGB());
            }
        }

        var byteOut = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteOut);
        return byteOut.toByteArray();
    }
}
