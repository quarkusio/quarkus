package io.quarkus.deployment.util;

import static io.quarkus.deployment.util.FileUtil.translateToVolumePath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FileUtilTest {

    @Test
    public void testTranslateToVolumePath() {
        // Windows-Style paths are formatted.
        assertEquals("//c/", translateToVolumePath("C"));
        assertEquals("//c/", translateToVolumePath("C:"));
        assertEquals("//c/", translateToVolumePath("C:\\"));
        assertEquals("//c/Users", translateToVolumePath("C:\\Users"));
        assertEquals("//c/Users/Quarkus/lambdatest-1.0-SNAPSHOT-native-image-source-jar",
                translateToVolumePath("C:\\Users\\Quarkus\\lambdatest-1.0-SNAPSHOT-native-image-source-jar"));

        // Side effect for Unix-style path.
        assertEquals("//c/Users/Quarkus", translateToVolumePath("c:/Users/Quarkus"));

        // Side effects for fancy inputs - for the sake of documentation.
        assertEquals("something/bizarre", translateToVolumePath("something\\bizarre"));
        assertEquals("something.bizarre", translateToVolumePath("something.bizarre"));
    }

}
