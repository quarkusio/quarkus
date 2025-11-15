package io.quarkus.devtools.testing;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.paths.MultiRootPathTree;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;

/**
 * Test file content and directory tree to make sure they are valid by comparing them to their snapshots.
 * The snapshots files can easily be updated when necessary and reviewed to confirm they are consistent with the changes.
 * <br />
 * <br />
 * The snapshots files will be created/updated using <code>-Dsnap</code> or
 * <code>-Dupdate-snapshots</code>
 * <br />
 * Snapshots are created in {@link #SNAPSHOTS_DIR}
 */
public class SnapshotTesting {

    // The PathTree API is used to support code start testing in the platform where snapshots are located in test JARs
    private static volatile PathTree snapshotsBaseRoot;
    private static final String SNAPSHOTS_DIR_NAME = "__snapshots__";

    public static final Path SNAPSHOTS_DIR = Path.of("src/test/resources").resolve(SNAPSHOTS_DIR_NAME);
    public static final String UPDATE_SNAPSHOTS_PROPERTY = "update-snapshots";
    public static final String UPDATE_SNAPSHOTS_PROPERTY_SHORTCUT = "snap";

    public static PathTree getSnapshotsBaseTree() {
        if (snapshotsBaseRoot != null) {
            return snapshotsBaseRoot;
        }

        PathTree srcTree = null;
        if (Files.isDirectory(SNAPSHOTS_DIR)) {
            srcTree = PathTree.ofDirectoryOrArchive(SNAPSHOTS_DIR.getParent());
        } else if (shouldUpdateSnapshot(SNAPSHOTS_DIR_NAME)) {
            try {
                Files.createDirectories(SNAPSHOTS_DIR);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            srcTree = PathTree.ofDirectoryOrArchive(SNAPSHOTS_DIR.getParent());
        }

        final URL url = Thread.currentThread().getContextClassLoader().getResource(SNAPSHOTS_DIR_NAME);
        if (url == null) {
            if (srcTree == null) {
                Assertions.fail("Failed to locate " + SNAPSHOTS_DIR_NAME + " directory on the classpath and "
                        + SNAPSHOTS_DIR.toAbsolutePath() + " directory does not exist (use -Dsnap to create it automatically)");
            }
            return snapshotsBaseRoot = srcTree;
        } else if ("file".equals(url.getProtocol())) {
            final Path p;
            try {
                p = Path.of(url.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Failed to translate " + url + " to path", e);
            }
            return snapshotsBaseRoot = new MultiRootPathTree(PathTree.ofDirectoryOrArchive(p.getParent()), srcTree);
        } else if ("jar".equals(url.getProtocol())) {
            final String jarUrlStr = url.toExternalForm();
            final String fileUrlStr = jarUrlStr.substring("jar:".length(),
                    jarUrlStr.length() - ("!/" + SNAPSHOTS_DIR_NAME).length());
            final Path p = Path.of(URI.create(fileUrlStr));
            final PathTree jarPathTree = PathTree.ofDirectoryOrArchive(p);
            return snapshotsBaseRoot = srcTree == null ? jarPathTree : new MultiRootPathTree(jarPathTree, srcTree);
        } else {
            throw new IllegalStateException("Unexpected URL protocol in " + url);
        }
    }

    public static <T> T withSnapshotsDir(String relativePath, Function<Path, T> function) {
        final PathTree snapshotsBaseRoot = getSnapshotsBaseTree();
        try (OpenPathTree tree = snapshotsBaseRoot.open()) {
            return function.apply(tree.getPath(SNAPSHOTS_DIR_NAME).resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open " + snapshotsBaseRoot.getRoots(), e);
        }
    }

    /**
     * Test file content to make sure it is valid by comparing it to its snapshots.
     * <br />
     * The snapshot file can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot file will be created/updated using <code>-Dsnap</code> or
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
     * The snapshot file can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot file will be created/updated using <code>-Dsnap</code> or
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
        assertThat(fileToCheck).isRegularFile();
        final boolean updateSnapshot = shouldUpdateSnapshot(snapshotIdentifier);
        return withSnapshotsDir(snapshotIdentifier, snapshotFile -> {
            if (updateSnapshot) {
                final Path srcSnapshotFile = SNAPSHOTS_DIR.resolve(snapshotIdentifier);
                if (Files.isRegularFile(srcSnapshotFile)) {
                    deleteExistingSnapshots(snapshotIdentifier, srcSnapshotFile);
                }
                try {
                    FileUtils.copyFile(fileToCheck.toFile(), srcSnapshotFile.toFile());
                    System.out.println("COPIED " + fileToCheck + " -> " + srcSnapshotFile);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                snapshotFile = srcSnapshotFile;
            }

            final String snapshotNotFoundDescription = "corresponding snapshot file not found for " + snapshotIdentifier
                    + " (Use -Dsnap to create it automatically)";
            final String description = "Snapshot is not matching (use -Dsnap to update it automatically): "
                    + snapshotIdentifier;
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
        });
    }

    /**
     * Test directory tree to make sure it is valid by comparing it to a snapshot.
     * <br />
     * The snapshot file can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot file will be created/updated using <code>-Dsnap</code> or
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
     * The snapshot file can easily be updated when necessary and reviewed to confirm it is consistent with the changes.
     * <br />
     * <br />
     * The snapshot file will be created/updated using <code>-Dsnap</code> or
     * <code>-Dupdate-snapshots</code>
     *
     * @param snapshotDirName the snapshot dir name for storage
     * @param dir the {@link Path} of the directory to test
     * @return a {@link ListAssert} with the directory tree as a list
     * @throws Throwable
     */
    public static ListAssert<String> assertThatDirectoryTreeMatchSnapshots(String snapshotDirName, Path dir) throws Throwable {
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

        final String snapshotName = snapshotDirName + "/dir-tree.snapshot";
        final boolean updateSnapshot = shouldUpdateSnapshot(snapshotName);

        return withSnapshotsDir(snapshotName, snapshotFile -> {
            try {
                if (updateSnapshot) {
                    final Path srcSnapshotFile = SNAPSHOTS_DIR.resolve(snapshotName);
                    if (Files.isRegularFile(srcSnapshotFile)) {
                        deleteExistingSnapshots(snapshotName, srcSnapshotFile);
                    }
                    Files.createDirectories(srcSnapshotFile.getParent());
                    Files.write(srcSnapshotFile, String.join("\n", tree).getBytes(StandardCharsets.UTF_8));
                    snapshotFile = srcSnapshotFile;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertThat(snapshotFile)
                    .as("corresponding snapshot file not found for " + snapshotName
                            + " (Use -Dsnap to create it automatically)")
                    .isRegularFile();

            final List<String> content = Arrays.stream(getTextContent(snapshotFile).split("\\v"))
                    .filter(s -> !s.isEmpty())
                    .collect(toList());

            return assertThat(tree)
                    .as("Snapshot is not matching (use -Dsnap to update it automatically):" + snapshotName)
                    .containsExactlyInAnyOrderElementsOf(content);
        });
    }

    public static String getTextContent(Path file) {
        try {
            return Files.readString(file);
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

    public static Consumer<Path> checkNotContains(String s) {
        return (p) -> assertThat(getTextContent(p)).doesNotContainIgnoringCase(s);
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
