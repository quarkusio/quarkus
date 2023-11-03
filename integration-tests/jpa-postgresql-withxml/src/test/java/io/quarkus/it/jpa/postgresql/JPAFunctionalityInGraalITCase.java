package io.quarkus.it.jpa.postgresql;

import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.ClassInclusionReport;

/**
 * Test various JPA operations running in native mode
 */
@QuarkusIntegrationTest
public class JPAFunctionalityInGraalITCase extends JPAFunctionalityTest {

    @Test
    public void verifyJDKXMLParsersAreIncluded() {
        final ClassInclusionReport report = ClassInclusionReport.load();
        //The following classes should be included in this applications;
        //if not, that would be a sign that this test has become too weak
        //to identify the well working of the exclusions.
        report.assertContains(org.postgresql.jdbc.PgSQLXML.class);
        report.assertContains(UUIDJdbcType.class);

        // And finally verify we included "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl" which is
        // the fallback implementation class name used in javax.xml.transform.TransformerFactory.newInstance()
        // whose invocation gets triggered when io.quarkus.jdbc.postgresql.runtime.graal.SQLXLMFeature is enabled.
        // We cannot use class javax.xml.transform.TransformerFactory directly since delegation to
        // the implementation might get inlined, thus resulting in 'javax.xml.transform.TransformerFactory'
        // not showing up as a used class in the reports (due to '-H:+InlineBeforeAnalysis').
        report.assertContains("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    }

}
