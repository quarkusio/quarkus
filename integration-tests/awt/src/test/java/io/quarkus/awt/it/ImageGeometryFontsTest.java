package io.quarkus.awt.it;

import static io.quarkus.awt.it.TestUtil.checkLog;
import static io.quarkus.awt.it.TestUtil.compareArrays;
import static io.quarkus.awt.it.TestUtil.decodeArray4;
import static io.restassured.RestAssured.given;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests generated geometry, primitive shapes, fonts,
 * including user provided .ttf TrueType fonts.
 */
@QuarkusTest
public class ImageGeometryFontsTest {

    /**
     * When comparing pixel colour values, how much difference
     * from the expected value is allowed.
     * 0 means no difference is tolerated.
     *
     * e.g. When building with -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-21-rhel8:latest
     * a different set of static libraries is used for HotSpot (system ones from RPM) and a different set
     * for Native Image (those bundled in JDK static libs installation).
     *
     * The discrepancy between imaging libraries could cause slight difference.
     * In this case, it is fonts antialiasing:
     *
     * TIFF: Wrong pixel. Expected: [46,14,32,255] Actual: [46,14,30,255]
     * GIF: Wrong pixel. Expected: [2,0,0,0] Actual: [1,0,0,0]
     * PNG: Wrong pixel. Expected: [46,14,32,255] Actual: [46,14,30,255]
     * JPG: Wrong pixel. Expected: [73,0,44,0] Actual: [72,0,39,0]
     * BMP: Wrong pixel. Expected: [46,14,32,0] Actual: [46,14,30,0]
     *
     * JPEG compression behaves differently between Linux and Windows,
     * so that also begs for pixel difference tolerance.
     *
     * Hence, the tolerance below is higher than 0:
     */
    private static final int[] PIXEL_DIFFERENCE_THRESHOLD_RGBA_VEC = new int[] { 25, 25, 40, 0 };

    @ParameterizedTest
    // @formatter:off
    @ValueSource(strings = {
    // Image format name followed by expected pixel values
    "TIFF █46,14,32,255 █229,195,83,255 █255,200,0,255 █255,0,0,255 █255,0,0,255",
    "GIF  █2,0,0,0      █214,0,0,0      █204,0,0,0     █121,0,0,0   █121,0,0,0",
    "PNG  █46,14,32,255 █229,195,83,255 █255,200,0,255 █255,0,0,255 █255,0,0,255",
    "JPG  █73,0,44,0    █225,162,170,0  █255,199,20,0  █239,7,0,0   █249,1,2,0",
    "BMP  █46,14,32,0   █229,195,83,0   █255,200,0,0   █255,0,0,0   █255,0,0,0",
    "WBMP █0,0,0,0      █0,0,0,0        █1,0,0,0       █0,0,0,0     █0,0,0,0"
    })
    // @formatter:on
    public void testGeometryAndFonts(String testData) throws IOException {
        final String[] formatPixels = testData.split("\\█");
        final String formatName = formatPixels[0].trim();
        final byte[] imgBytes = given()
                .when()
                .get("/graphics/" + formatName)
                .asByteArray();
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imgBytes));

        // Sanity
        Assertions.assertNotNull(image, formatName + ": The data returned are not a valid image.");
        Assertions.assertTrue(image.getWidth() == 350 && image.getHeight() == 300,
                String.format("%s: image's expected dimension is %d x %d, but was %d x %d.",
                        formatName, 350, 300, image.getWidth(), image.getHeight()));

        // Test pixels
        final int[][] pixelsCoordinates = new int[][] { { 80, 56 }, { 34, 103 }, { 50, 186 }, { 246, 204 }, { 294, 205 } };
        for (int i = 0; i < pixelsCoordinates.length; i++) {
            final int[] expected = decodeArray4(formatPixels[i + 1].trim());
            final int[] actual = new int[4]; //4BYTE RGBA
            image.getData().getPixel(pixelsCoordinates[i][0], pixelsCoordinates[i][1], actual);
            Assertions.assertTrue(compareArrays(expected, actual, PIXEL_DIFFERENCE_THRESHOLD_RGBA_VEC),
                    String.format("%s: Wrong pixel at %dx%d. Expected: [%d,%d,%d,%d] Actual: [%d,%d,%d,%d]",
                            formatName, pixelsCoordinates[i][0], pixelsCoordinates[i][1],
                            expected[0], expected[1], expected[2], expected[3],
                            actual[0], actual[1], actual[2], actual[3]));
        }
        checkLog(null, "Geometry and Fonts");
    }

    /**
     * At least some system fonts are expected to be found. The number may vary wildly
     * depending on the system, so it doesn't make sense to be checking for a particular
     * number or even for font family names.
     */
    @Test
    void checkFonts() {
        final String fonts = given()
                .when()
                .get("/fonts")
                .asString();
        final String[] actual = fonts.substring(1, fonts.length() - 1).split(", ");
        Assertions.assertTrue(actual.length > 3,
                "There are supposed to be at least some system fonts found beside those installed by this TS.");
        final Map<String, Boolean> expected = new HashMap<>(Map.of(
                "MyFreeMono", Boolean.FALSE,
                "MyFreeSerif", Boolean.FALSE,
                "DejaVu Sans Mono X", Boolean.FALSE));
        for (String f : actual) {
            expected.replace(f, Boolean.TRUE);
        }
        Assertions.assertTrue(expected.values().stream().allMatch(Boolean.TRUE::equals),
                "Not all expected fonts were found: " + expected + ". " +
                        "These fonts were found though: " + Arrays.toString(actual));
    }
}
