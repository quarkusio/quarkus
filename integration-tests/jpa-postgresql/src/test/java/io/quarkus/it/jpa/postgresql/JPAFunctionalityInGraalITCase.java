package io.quarkus.it.jpa.postgresql;

import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.NativeImageReport;

/**
 * Test various JPA operations running in native mode
 */
@QuarkusIntegrationTest
public class JPAFunctionalityInGraalITCase extends JPAFunctionalityTest {

    private static final int EXPECTED_NUMBER_OF_USED_CLASSES = 12400;
    private static final int EXPECTED_NUMBER_OF_USED_METHODS = 80000;
    private static final int EXPECTED_NUMBER_OF_USED_PACKAGES = 800;
    private static final int THRESHOLD_PERCENTAGE = 3;

    @Test
    public void verifyJdkXmlParsersHavebeenEcludedFromNative() {
        final NativeImageReport usedClassesReport = NativeImageReport.load(NativeImageReport.ReportType.USED_CLASSES);
        //The following classes should be included in this applications;
        //if not, that would be a sign that this test has become too weak
        //to identify the well working of the exclusions.
        usedClassesReport.assertContains(org.postgresql.jdbc.PgSQLXML.class);
        usedClassesReport.assertContains(UUIDJdbcType.class);

        // And finally verify we exclude "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl" which is
        // the fallback implementation class name used in javax.xml.transform.TransformerFactory.newInstance()
        // whose invocation gets triggered when io.quarkus.jdbc.postgresql.runtime.graal.SQLXLMFeature is enabled.
        // We cannot only use class javax.xml.transform.TransformerFactory directly since delegation to
        // the implementation might get inlined, thus resulting in 'javax.xml.transform.TransformerFactory'
        // not showing up as a used class in the reports (due to '-H:+InlineBeforeAnalysis').
        usedClassesReport.assertContainsNot(javax.xml.transform.TransformerFactory.class);
        usedClassesReport.assertContainsNot("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");

        usedClassesReport.assertSizeWithingThreshold(EXPECTED_NUMBER_OF_USED_CLASSES, THRESHOLD_PERCENTAGE);
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
