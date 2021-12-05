package io.quarkus.it.spring.data.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class PropertyWarningsPMT {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(FruitResource.class))
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
        assertThat(buildLogRecords).singleElement().satisfies(r -> {
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
                                    "spring.jpa.hibernate.naming.implicit-strategy should be replaced by quarkus.hibernate-orm.implicit-naming-strategy")
                            .contains(
                                    "quarkus.hibernate-orm.sql-load-script could be used to load data instead of spring.datasource.data but it does not support either comma separated list of resources or resources with ant-style patterns as spring.datasource.data does, it accepts the name of the file containing the SQL statements to execute when when Hibernate ORM starts");
                });
            });
        });
    }
}
