package io.quarkus.it.jpa.postgresql;

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
        report.assertContains(org.hibernate.type.PostgresUUIDType.class);

        //And finally verify we included the JDK XML by triggering
        //io.quarkus.jdbc.postgresql.runtime.graal.SQLXLMFeature
        report.assertContains(javax.xml.transform.TransformerFactory.class);
    }

}
