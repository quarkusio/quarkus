package io.quarkus.awt.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Checking logs, support methods, utils.
 */
public class TestUtil {

    /**
     * These exceptions in native-image runtime are a hallmark of either
     * JNI access or reflection misconfiguration. Test should always fail the
     * logs check if these are found.
     */
    private static final Pattern BLACKLISTED_EXCEPTIONS = Pattern.compile("(?i:.*(" +
            "java.lang.NoSuchFieldError|" +
            "java.lang.NoClassDefFoundError|" +
            "java.lang.NullPointerException" +
            ").*)");

    /**
     * Looks for a pattern in the log or just seeks blacklisted errors.
     *
     * @param lineMatchRegexp pattern
     * @param name identifier
     */
    public static void checkLog(final Pattern lineMatchRegexp, final String name) {
        final Path accessLogFilePath = Paths.get(".", "target", "quarkus.log").toAbsolutePath();
        org.awaitility.Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTrue(Files.exists(accessLogFilePath), "access log file " + accessLogFilePath + " is missing");
                    boolean found = false;
                    final StringBuilder sbLog = new StringBuilder();
                    final Set<String> offendingLines = new HashSet<>();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(accessLogFilePath)),
                                    StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (BLACKLISTED_EXCEPTIONS.matcher(line).matches()) {
                                offendingLines.add(line);
                            }
                            sbLog.append(line).append("\r\n");
                            if (lineMatchRegexp != null) {
                                found = lineMatchRegexp.matcher(line).matches();
                                if (found) {
                                    break;
                                }
                            }
                        }
                    }
                    assertTrue(offendingLines.isEmpty(),
                            name + ": Log file must not contain blacklisted exceptions. " +
                                    "See the offending lines: \n" +
                                    String.join("\n", offendingLines) +
                                    "\n in the context of the log: " + sbLog);
                    if (lineMatchRegexp != null) {
                        assertTrue(found,
                                name + ": Log file doesn't contain a line matching " + lineMatchRegexp.pattern() +
                                        ", log was: " + sbLog);
                    }
                });
    }

    /**
     * Compares two int arrays, pair by pair. If the difference
     * between members of the pair is bigger than threshold,
     * arrays are not the same.
     *
     * @param a array
     * @param b array
     * @param threshold array tolerance for the absolute difference between array elements
     * @return true if they are the same (within the threshold)
     */
    public static boolean compareArrays(int[] a, int[] b, int[] threshold) {
        if (a.length != b.length || a.length != threshold.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Math.max(a[i], b[i]) - Math.min(a[i], b[i]) > threshold[i]) {
                return false;
            }
        }
        return true;
    }

    public static int[] decodeArray4(final String array) {
        final String[] ints = array.split(",");
        return new int[] {
                Integer.parseInt(ints[0]),
                Integer.parseInt(ints[1]),
                Integer.parseInt(ints[2]),
                Integer.parseInt(ints[3])
        };
    }
}
