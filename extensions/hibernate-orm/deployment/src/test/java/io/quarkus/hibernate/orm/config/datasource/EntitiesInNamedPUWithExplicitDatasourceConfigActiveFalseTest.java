package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.config.namedpu.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInNamedPUWithExplicitDatasourceConfigActiveFalseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(MyEntity.class.getPackage().getName()))
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.datasource.\"ds-1\".active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Unable to find datasource 'ds-1' for persistence unit 'pu-1'",
                            "Datasource 'ds-1' was deactivated through configuration properties.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.\"ds-1\".active'"
                                    + " to 'true' and configure datasource 'ds-1'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
