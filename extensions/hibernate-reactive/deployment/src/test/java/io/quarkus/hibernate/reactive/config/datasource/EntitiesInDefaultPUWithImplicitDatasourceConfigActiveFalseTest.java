package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithImplicitDatasourceConfigActiveFalseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.active", "false")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "Unable to find datasource '<default>' for persistence unit 'default-reactive'",
                            "Datasource '<default>' was deactivated through configuration properties.",
                            "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                            "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.active'"
                                    + " to 'true' and configure datasource '<default>'",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
