package io.quarkus.it.spring.data.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class PropertyWarningsPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(FruitResource.class))
            .setApplicationName("property-warnings")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setLogRecordPredicate(r -> "io.quarkus.spring.data.deployment.SpringDataJPAProcessor".equals(r.getLoggerName()))
            .withConfigurationResource("property-warnings-test.properties");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void ensureProperQuarkusPropertiesLogged() {
        List<LogRecord> buildLogRecords = prodModeTestResults.getRetainedBuildLogRecords();
        assertThat(buildLogRecords).isNotEmpty();
        assertThat(buildLogRecords).hasOnlyOneElementSatisfying(r -> {
            assertThat(r.getMessage()).contains("Quarkus does not support the following Spring Boot configuration properties");
            assertThat(r.getParameters()).hasOnlyOneElementSatisfying(o -> {
                assertThat(o).isInstanceOfSatisfying(String.class, s -> {
                    assertThat(s).contains("spring.jpa.show-sql should be replaced by quarkus.hibernate-orm.log.sql")
                            .contains(
                                    "spring.jpa.properties.hibernate.dialect should be replaced by quarkus.hibernate-orm.dialect")
                            .contains("spring.jpa.open-in-view")
                            .contains(
                                    "spring.jpa.hibernate.naming.physical-strategy should be replaced by quarkus.hibernate-orm.physical-naming-strategy")
                            .contains(
                                    "spring.jpa.hibernate.naming.implicit-strategy should be replaced by quarkus.hibernate-orm.implicit-naming-strategy");
                });
            });
        });
    }
}
