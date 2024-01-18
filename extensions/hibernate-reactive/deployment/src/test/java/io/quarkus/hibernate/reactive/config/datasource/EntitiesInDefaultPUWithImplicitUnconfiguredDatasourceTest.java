package io.quarkus.hibernate.reactive.config.datasource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithImplicitUnconfiguredDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void testInvalidConfiguration() {
        // bootstrap will succeed and ignore the fact that a datasource is unconfigured...
    }

}
