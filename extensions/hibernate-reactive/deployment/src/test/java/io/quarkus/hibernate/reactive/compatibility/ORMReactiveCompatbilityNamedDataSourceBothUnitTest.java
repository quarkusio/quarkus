package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ORMReactiveCompatbilityNamedDataSourceBothUnitTest extends CompatibilityUnitTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()) // this triggers Agroal
            ))
            .withConfigurationResource("application-unittest-both-named.properties")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", SCHEMA_MANAGEMENT_STRATEGY)
            .overrideConfigKey("quarkus.hibernate-orm.datasource", "named-datasource")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".reactive", "true")
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".db-kind", POSTGRES_KIND)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".username", USERNAME_PWD)
            .overrideConfigKey("quarkus.datasource.\"named-datasource\".password", USERNAME_PWD)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            // Hibernate Reactive doesn't support explicitly setting the datasource (yet),
                            // so it will just notice the default datasource is not configured!
                            "The default datasource must be configured for Hibernate Reactive",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void test() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
