package io.quarkus.hibernate.orm.config.namedpu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInNamedPUWithExplicitUnconfiguredDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                assertThat(t)
                        .isInstanceOf(ConfigurationException.class)
                        .hasMessageContainingAll(
                                "The datasource 'ds-1' is not configured but the persistence unit 'pu-1' uses it.",
                                "To solve this, configure datasource 'ds-1'.",
                                "Refer to https://quarkus.io/guides/datasource for guidance.");
            })
            .withApplicationRoot((jar) -> jar
                    .addPackage(MyEntity.class.getPackage().getName())
                    .addAsResource("application-named-pu-explicit-unconfigured-datasource.properties",
                            "application.properties"));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
