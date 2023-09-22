package io.quarkus.it.main;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.NativeImageReport;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class UsedCodeReportITCase {

    private static final int EXPECTED_NUMBER_OF_USED_CLASSES = 21000;
    private static final int EXPECTED_NUMBER_OF_USED_METHODS = 136000;
    private static final int EXPECTED_NUMBER_OF_USED_PACKAGES = 1450;
    private static final int THRESHOLD_PERCENTAGE = 3;

    @Test
    public void verifyUsedClassesNumber() {
        final NativeImageReport usedMethodsReport = NativeImageReport.load(NativeImageReport.ReportType.USED_CLASSES);
        usedMethodsReport.assertSizeWithingThreshold(EXPECTED_NUMBER_OF_USED_CLASSES, THRESHOLD_PERCENTAGE);
    }

    @Test
    public void verifyUsedMethodsNumber() {
        final NativeImageReport usedMethodsReport = NativeImageReport.load(NativeImageReport.ReportType.USED_METHODS);
        usedMethodsReport.assertSizeWithingThreshold(EXPECTED_NUMBER_OF_USED_METHODS, THRESHOLD_PERCENTAGE);
    }

    @Test
    public void verifyUsedPackagesNumber() {
        final NativeImageReport usedPackagesReport = NativeImageReport.load(NativeImageReport.ReportType.USED_PACKAGES);
        usedPackagesReport.assertSizeWithingThreshold(EXPECTED_NUMBER_OF_USED_PACKAGES, THRESHOLD_PERCENTAGE);
    }

}
