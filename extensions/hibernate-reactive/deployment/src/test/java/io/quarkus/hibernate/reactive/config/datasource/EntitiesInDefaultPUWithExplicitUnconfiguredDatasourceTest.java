package io.quarkus.hibernate.reactive.config.datasource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithExplicitUnconfiguredDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "drop-and-create");

    @Test
    public void testInvalidConfiguration() {
        // bootstrap will succeed and ignore the fact that a datasource is unconfigured...
    }

}
