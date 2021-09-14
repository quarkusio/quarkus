package io.quarkus.test.junit.nativeimage;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;

/**
 * This is a general utility to assert via
 * unit testing which classes have been included in a native-image.
 */
public final class ClassInclusionReport {

    private final Set<String> includedClasses;
    private final Path usedClassesReport;

    private ClassInclusionReport(Set<String> includedClasses, Path usedClassesReport) {
        this.includedClasses = includedClasses;
        this.usedClassesReport = usedClassesReport;
    }

    /**
     * This will load the class inclusions report assuming the native-image
     * was built in the current module and following Maven conventions:
     * by walking into the '/target' directory from the current directory.
     *
     * @return An assertable report of all classes included in the current app
     */
    public static ClassInclusionReport load() {
        final Path usedClassesReport = getUsedClassesReport();
        TreeSet<String> set = new TreeSet<>();
        try (Scanner scanner = new Scanner(usedClassesReport.toFile())) {
            while (scanner.hasNextLine()) {
                set.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not load used classes report", e);
        }
        return new ClassInclusionReport(set, usedClassesReport);
    }

    public void assertContains(final Class<?> type) {
        assertContains(type.getName());
    }

    public void assertContainsNot(final Class<?> type) {
        assertContainsNot(type.getName());
    }

    public void assertContains(final String typeName) {
        final boolean contains = includedClasses.contains(typeName);
        if (!contains) {
            Assertions.fail(
                    "Type '" + typeName + "' was not found in the report in " + usedClassesReport);
        }
    }

    public void assertContainsNot(final String typeName) {
        final boolean contains = includedClasses.contains(typeName);
        if (contains) {
            Assertions.fail("Type '" + typeName + "' was found in the report in " + usedClassesReport);
        }
    }

    private static Path getUsedClassesReport() {
        final Path reportsPath = nativeImageReportsPath();
        final File[] usedClassesReports = reportsPath.toFile().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT)
                .startsWith("used_classes_"));
        Assertions.assertNotNull(usedClassesReports, "Could not identify the native image build directory");
        Assertions.assertEquals(1, usedClassesReports.length, "Could not identify the native image build directory");
        return usedClassesReports[0].toPath();
    }

    private static Path nativeImageReportsPath() {
        final Path nativeBuildPath = locateNativeImageBuildDirectory();
        final Path reportsPath = nativeBuildPath.resolve("reports");
        Assertions.assertTrue(reportsPath.toFile().exists(),
                "The reports directory doesn't exist?! Make sure this build was invoked with 'quarkus.native.enable-reports=true'");
        return reportsPath;
    }

    private static Path locateNativeImageBuildDirectory() {
        Path buildPath = Paths.get("target");
        final File[] files = buildPath.toFile().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT)
                .endsWith("-native-image-source-jar"));
        Assertions.assertNotNull(files, "Could not identify the native image build directory");
        Assertions.assertEquals(1, files.length, "Could not identify the native image build directory");
        return files[0].toPath();
    }
}
