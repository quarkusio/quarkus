package io.quarkus.devtools.testing;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;

/**
 * Test file content and directory tree to make sure they are valid by comparing them to their snapshots.
 * The snapshots can easily be updated when necessary and reviewed to confirm they are consistent with the changes.
 * <br />
 * <br />
 * The snapshots will be created/updated using <code>-Dsnap</code> or
 * <code>-Dupdate-snapshots</code>
 * <br />
 * Snapshots are created in {@link #SNAPSHOTS_DIR}
 */
public class SnapshotTesting {

    public static final Path SNAPSHOTS_DIR = Paths.get("src/test/resources/__snapshots__/");
    public static final String UPDATE_SNAPSHOTS_PROPERTY = "update-snapshots";
    public static final String UPDATE_SNAPSHOTS_PROPERTY_SHORTCUT = "snap";

    /**
     * Test file content to make sure it is valid by comparing it to its snapshots.
     * <br />
     * The snapshot can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot will be created/updated using <code>-Dsnap</code> or
     * <code>-Dupdate-snapshots</code>
     * <br />
     * <br />
     * Even if the content is checked as a whole, it's always better to also manually check that specific content snippets
     * contains what's expected
     * <br />
     * <br />
     * example:
     *
     * <pre>
     * assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java")
     *         .satisfies(checkContains("@Path(\"/hello\")"))
     * </pre>
     *
     * @param testInfo the {@link TestInfo} from the {@Link Test} parameter (used to get the current test class & method to
     *        compute the snapshot location)
     * @param parentDir the parent directory containing the generated files for this test (makes it nicer when checking multiple
     *        snapshots)
     * @param fileRelativePath the relative path from the directory (used to name the snapshot)
     * @return an {@link AbstractPathAssert} giving a direct way to check specific content snippets contains what's expected
     * @throws Throwable
     */
    public static AbstractPathAssert<?> assertThatMatchSnapshot(TestInfo testInfo, Path parentDir, String fileRelativePath)
            throws Throwable {
        final String snapshotDirName = getSnapshotDirName(testInfo);
        final String normalizedFileName = snapshotDirName + "/" + normalizePathAsName(fileRelativePath);
        return assertThatMatchSnapshot(parentDir.resolve(fileRelativePath), normalizedFileName);
    }

    /**
     * Test file content to make sure it is valid by comparing it to a snapshot.
     * <br />
     * The snapshot can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot will be created/updated using <code>-Dsnap</code> or
     * <code>-Dupdate-snapshots</code>
     * <br />
     * <br />
     * Even if the content is checked as a whole, it's always better to also manually check that specific content snippets
     * contains what's expected using {@link #checkContains(String)} or {@link #checkMatches(String)}
     *
     * @param fileToCheck the {@link Path} of the file to check
     * @param snapshotIdentifier the snapshotIdentifier of the snapshot (used as a relative path from the {@link #SNAPSHOTS_DIR}
     * @return an {@link AbstractPathAssert} giving a direct way to check specific content snippets contains what's expected
     * @throws Throwable
     */
    public static AbstractPathAssert<?> assertThatMatchSnapshot(Path fileToCheck, String snapshotIdentifier) throws Throwable {
        final Path snapshotFile = SNAPSHOTS_DIR.resolve(snapshotIdentifier);
        assertThat(fileToCheck).isRegularFile();

        final boolean updateSnapshot = shouldUpdateSnapshot(snapshotIdentifier);

        if (updateSnapshot) {
            if (Files.isRegularFile(snapshotFile)) {
                deleteExistingSnapshots(snapshotIdentifier, snapshotFile);
            }
            FileUtils.copyFile(fileToCheck.toFile(), snapshotFile.toFile());
        }

        final String snapshotNotFoundDescription = "corresponding snapshot not found for " + snapshotIdentifier
                + " (Use -Dsnap to create it automatically)";
        final String description = "Snapshot is not matching (use -Dsnap to udpate it automatically): " + snapshotIdentifier;
        if (isUTF8File(fileToCheck)) {
            assertThat(snapshotFile).as(snapshotNotFoundDescription).isRegularFile();
            assertThat(fileToCheck).as(description).exists()
                    .usingCharset(StandardCharsets.UTF_8)
                    .hasSameTextualContentAs(snapshotFile, StandardCharsets.UTF_8);

        } else {
            assertThat(snapshotFile).as(snapshotNotFoundDescription).isRegularFile();
            assertThat(fileToCheck).as(description).hasSameBinaryContentAs(snapshotFile);
        }
        return assertThat(fileToCheck);
    }

    /**
     * Test directory tree to make sure it is valid by comparing it to a snapshot.
     * <br />
     * The snapshot can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot will be created/updated using <code>-Dsnap</code> or
     * <code>-Dupdate-snapshots</code>
     *
     * @param testInfo the {@link TestInfo} from the {@Link Test} parameter (used to get the current test class & method to
     *        compute the snapshot location)
     * @param dir the {@link Path} of the directory to test
     * @return a {@link ListAssert} with the directory tree as a list
     * @throws Throwable
     */
    public static ListAssert<String> assertThatDirectoryTreeMatchSnapshots(TestInfo testInfo, Path dir) throws Throwable {
        return assertThatDirectoryTreeMatchSnapshots(getSnapshotDirName(testInfo), dir);
    }

    /**
     * Test directory tree to make sure it is valid by comparing it to a snapshot.
     * <br />
     * The snapshot can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot will be created/updated using <code>-Dsnap</code> or
     * <code>-Dupdate-snapshots</code>
     *
     * @param snapshotDirName the snapshot dir name for storage
     * @param dir the {@link Path} of the directory to test
     * @return a {@link ListAssert} with the directory tree as a list
     * @throws Throwable
     */
    public static ListAssert<String> assertThatDirectoryTreeMatchSnapshots(String snapshotDirName, Path dir) throws Throwable {
        final String snapshotName = snapshotDirName + "/dir-tree.snapshot";
        final Path snapshotFile = SNAPSHOTS_DIR.resolve(snapshotName);

        assertThat(dir).isDirectory();

        final List<String> tree = Files.walk(dir)
                .map(p -> {
                    final String r = dir.relativize(p).toString().replace('\\', '/');
                    if (Files.isDirectory(p)) {
                        return r + "/";
                    }
                    return r;
                })
                .sorted()
                .collect(toList());

        final boolean updateSnapshot = shouldUpdateSnapshot(snapshotName);

        if (updateSnapshot) {
            if (Files.isRegularFile(snapshotFile)) {
                deleteExistingSnapshots(snapshotName, snapshotFile);
            }
            Files.createDirectories(snapshotFile.getParent());
            Files.write(snapshotFile, String.join("\n", tree).getBytes(StandardCharsets.UTF_8));
        }

        assertThat(snapshotFile)
                .as("corresponding snapshot not found for " + snapshotName + " (Use -Dsnap to create it automatically)")
                .isRegularFile();

        final List<String> content = Arrays.stream(getTextContent(snapshotFile).split("\\v"))
                .filter(s -> !s.isEmpty())
                .collect(toList());

        return assertThat(tree)
                .as("Snapshot is not matching (use -Dsnap to udpate it automatically):" + snapshotName)
                .containsExactlyInAnyOrderElementsOf(content);
    }

    public static String getTextContent(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file.toString(), e);
        }
    }

    public static void deleteTestDirectory(final File file) throws IOException {
        FileUtils.deleteDirectory(file);

        Assertions.assertFalse(
                Files.exists(file.toPath()), "Directory still exists");
    }

    /**
     * To use with {@link AbstractPathAssert} in order to check the file content contains a specific string.
     *
     * @param s the string which should be in the file content
     * @return a {@link Consumer<Path>} to use with {@link AbstractPathAssert#satisfies(Consumer)}
     */
    public static Consumer<Path> checkContains(String s) {
        return (p) -> assertThat(getTextContent(p)).contains(s);
    }

    public static Consumer<Path> checkMatches(String regex) {
        return (p) -> assertThat(getTextContent(p)).matches(regex);
    }

    public static String getSnapshotDirName(TestInfo testInfo) {
        return testInfo.getTestClass().get().getSimpleName() + '/' + testInfo.getTestMethod().get().getName();
    }

    public static String normalizePathAsName(String fileRelativePath) {
        return fileRelativePath.replace('/', '_');
    }

    private static boolean shouldUpdateSnapshot(String identifier) {
        return getUpdateSnapshotsProp().filter(u -> u.isEmpty() || "true".equalsIgnoreCase(u) || u.contains(identifier))
                .isPresent();
    }

    private static boolean isUTF8File(final Path file) {
        try {
            final byte[] inputBytes = Files.readAllBytes(file);
            final String converted = new String(inputBytes, StandardCharsets.UTF_8);
            final byte[] outputBytes = converted.getBytes(StandardCharsets.UTF_8);
            return Arrays.equals(inputBytes, outputBytes);
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteExistingSnapshots(String name, Path snapshots) {
        System.out.println("\n>>>>>> DELETING EXISTING TEST SNAPSHOTS FOR:\n>>>>>> " + name + "\n");
        FileUtils.deleteQuietly(snapshots.toFile());
    }

    static Optional<String> getUpdateSnapshotsProp() {
        final Optional<String> property = Optional
                .ofNullable(System.getProperty(UPDATE_SNAPSHOTS_PROPERTY, System.getenv(UPDATE_SNAPSHOTS_PROPERTY)));
        if (property.isPresent()) {
            return property;
        }
        return Optional.ofNullable(
                System.getProperty(UPDATE_SNAPSHOTS_PROPERTY_SHORTCUT, System.getenv(UPDATE_SNAPSHOTS_PROPERTY_SHORTCUT)));
    }

}
