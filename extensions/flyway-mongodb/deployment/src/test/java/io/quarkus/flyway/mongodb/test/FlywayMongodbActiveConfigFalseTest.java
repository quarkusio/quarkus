package io.quarkus.flyway.mongodb.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusExtensionTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbActiveConfigFalseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1__create_users.json", "db/migration/V1__create_users.json"))
            .overrideConfigKey("quarkus.mongodb.connection-string", FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING)
            .overrideConfigKey("quarkus.mongodb.database", "acf")
            .overrideConfigKey("quarkus.flyway-mongodb.active", "false");

    @Inject
    Instance<Flyway> flyway;

    @Test
    @DisplayName("When active=false the app boots but the Flyway bean is inactive")
    void flywayBeanInactiveWhenActiveFalse() {
        assertThatThrownBy(flyway::get)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Flyway MongoDB for client '<default>' was deactivated through configuration.",
                        "quarkus.flyway-mongodb.active");
    }
}
