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
 * unit testing which and how many classes, methods, or packages have been included in a native-image.
 */
public final class NativeImageReport {

    private final Set<String> lines;
    private final Path report;
    private final ReportType reportType;

    private NativeImageReport(Set<String> includedClasses, Path usedClassesReport, ReportType reportType) {
        this.lines = includedClasses;
        this.report = usedClassesReport;
        this.reportType = reportType;
    }

    /**
     * This will load the class inclusions report assuming the native-image
     * was built in the current module and following Maven conventions:
     * by walking into the '/target' directory from the current directory.
     *
     * @return An assertable report of all classes included in the current app
     */
    public static NativeImageReport load(ReportType reportType) {
        final Path report = getReport(reportType);
        TreeSet<String> set = new TreeSet<>();
        try (Scanner scanner = new Scanner(report.toFile())) {
            while (scanner.hasNextLine()) {
                set.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not load used classes report", e);
        }
        return new NativeImageReport(set, report, reportType);
    }

    public void assertContains(final Class<?> line) {
        assertContains(line.getName());
    }

    public void assertContainsNot(final Class<?> line) {
        assertContainsNot(line.getName());
    }

    public void assertContains(final String line) {
        final boolean contains = lines.contains(line);
        if (!contains) {
            Assertions.fail(
                    "Type '" + line + "' was not found in the report in " + report);
        }
    }

    public void assertContainsNot(final String line) {
        final boolean contains = lines.contains(line);
        if (contains) {
            Assertions.fail("Type '" + line + "' was found in the report in " + report);
        }
    }

    private static Path getReport(ReportType reportType) {
        final Path reportsPath = nativeImageReportsPath();
        final File[] usedClassesReports = reportsPath.toFile().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT)
                .startsWith(reportType + "_"));
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

    /**
     * Asserts that the number of lines in the report is within a certain threshold
     * of the expected number of lines.
     *
     * @param expectedNumberOfLInes The expected number of lines in the report
     * @param thesholdPercentage The threshold percentage
     */
    public void assertSizeWithingThreshold(int expectedNumberOfLInes, int thesholdPercentage) {
        final int actualNumberOfLines = lines.size();
        final int threshold = expectedNumberOfLInes * thesholdPercentage / 100;
        Assertions.assertTrue(actualNumberOfLines >= expectedNumberOfLInes - threshold,
                "The number of lines in the " + reportType + " report is too low: " + actualNumberOfLines + " vs. "
                        + expectedNumberOfLInes);
        Assertions.assertTrue(actualNumberOfLines <= expectedNumberOfLInes + threshold,
                "The number of lines in the " + reportType + " report is too high: " + actualNumberOfLines + " vs. "
                        + expectedNumberOfLInes);
    }

    public enum ReportType {
        USED_CLASSES,
        USED_METHODS,
        USED_PACKAGES;

        public String toString() {
            return name().toLowerCase();
        }
    }
}
