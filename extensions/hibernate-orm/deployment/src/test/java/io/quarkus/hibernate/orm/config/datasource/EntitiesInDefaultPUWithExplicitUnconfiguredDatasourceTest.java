package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.config.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithExplicitUnconfiguredDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "drop-and-create")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Unable to find datasource 'ds-1' for persistence unit '<default>'",
                            "Datasource 'ds-1' is not configured.",
                            "To solve this, configure datasource 'ds-1'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
