package io.quarkus.awt.it;

import static io.quarkus.awt.it.TestUtil.checkLog;
import static io.quarkus.awt.it.TestUtil.compareArrays;
import static io.quarkus.awt.it.TestUtil.decodeArray4;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests image encoders.
 */
@QuarkusTest
public class ImageEncodersTest {

    /**
     * When comparing pixel colour values, how much difference
     * from the expected value is allowed.
     * 0 means no difference is tolerated.
     */
    private static final int[] PIXEL_DIFFERENCE_THRESHOLD_RGBA_VEC = new int[] { 2, 2, 2, 0 };

    /**
     * //@formatter:off
     * Document format in encoders_test_config.txt.
     * e.g.
     *
     * TYPE_4BYTE_ABGR/TIFF/CS_PYCC/ZLib|255,0,0,255|0,0,255,255
     * │              │    │       │    │           └── Second pixel values to test at x:2 y:2
     * │              │    │       │    └── First pixel values to test at x:25 y:25.
     * │              │    │       └── Compression algorithm.
     * │              │    └── Color space used to construct color model.
     * │              └── Image file format, container.
     * └── Image type, how does it actually store pixels.
     *
     * Why aren't *all* combinations tested?
     * Because only a subset is valid, e.g. certain image file formats allow
     * certain compressions, certain colour spaces make no sense with
     * given image types etc.
     *
     * If testing pixel values fails, it might look as e.g:
     *      org.opentest4j.AssertionFailedError:
     *      There were errors verifying image data, see:
     *      TYPE_INT_RGB/BMP/CS_LINEAR_RGB/BI_BITFIELDS Wrong pixel. Expected: [0,0,255,10] Actual: [0,0,255,0]
     *
     * //@formatter:on
     */
    @Test
    public void testEncoders() throws IOException {
        final List<String> errors = new ArrayList<>();
        try (Scanner sc = new Scanner(
                new File(ImageEncodersTest.class.getResource("/encoders_test_config.txt").getFile()), UTF_8)) {
            while (sc.hasNextLine()) {
                final String line = sc.nextLine();
                final String[] segments = line.split("\\|");
                // Config for the image encoder, type, format, color space, compression
                final String path = segments[0];
                final byte[] imgBytes = given()
                        .when()
                        .get("/generate/" + path)
                        .asByteArray();
                final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imgBytes));

                // Check the image is actually readable.
                if (image == null) {
                    errors.add(path + " Failed to generate an image.");
                    continue;
                }

                // Check two pixels. One in the center and one in a corner.
                final int[][] pixelsCoordinates = new int[][] { { 25, 25 }, { 2, 2 } };
                for (int i = 0; i < pixelsCoordinates.length; i++) {
                    final int[] expected = decodeArray4(segments[i + 1]);
                    final int[] actual = new int[4]; //4BYTE RGBA
                    image.getData().getPixel(pixelsCoordinates[i][0], pixelsCoordinates[i][1], actual);
                    if (!compareArrays(expected, actual, PIXEL_DIFFERENCE_THRESHOLD_RGBA_VEC)) {
                        errors.add(String.format("%s: Wrong pixel at %dx%d. Expected: [%d,%d,%d,%d] Actual: [%d,%d,%d,%d]",
                                path, pixelsCoordinates[i][0], pixelsCoordinates[i][1],
                                expected[0], expected[1], expected[2], expected[3],
                                actual[0], actual[1], actual[2], actual[3]));
                    }
                }
            }
        }
        Assertions.assertTrue(errors.isEmpty(),
                "There were errors verifying image data, see:\n" + String.join("\n", errors) + "\n");
        checkLog(null, "Encoders");
    }
}
