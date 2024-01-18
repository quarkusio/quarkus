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
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.database.generation", "drop-and-create")
            .overrideConfigKey("quarkus.datasource.\"ds-1\".active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Unable to find datasource 'ds-1' for persistence unit 'pu-1'",
                            "Datasource 'ds-1' was deactivated through configuration properties.",
                            "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                            "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"ds-1\".active'"
                                    + " to 'true' and configure datasource 'ds-1'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
