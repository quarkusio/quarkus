package io.quarkus.hibernate.reactive.config.datasource;

import static io.quarkus.datasource.common.runtime.DatabaseKind.POSTGRESQL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithExplicitDatasourceConfigUrlMissingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", POSTGRESQL)
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            // Hibernate Reactive doesn't support explicitly setting the datasource (yet),
                            // so it will just notice the datasource is not configured!
                            "Datasource 'ds-1' was deactivated automatically because its URL is not set. To activate the datasource, set configuration property 'quarkus.datasource.\"ds-1\".reactive.url'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
