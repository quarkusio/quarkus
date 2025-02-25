package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionMigrateAtStartDefaultDatasourceConfigActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql"))
            .overrideConfigKey("quarkus.datasource.active", "false")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "true");

    @Inject
    Instance<Flyway> flyway;

    @Test
    @DisplayName("If the default datasource is deactivated, even if migrate-at-start is enabled, the application should boot, but Flyway should be deactivated for that datasource")
    public void testBootSucceedsButFlywayDeactivated() {
        assertThatThrownBy(flyway::get)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Flyway for datasource '<default>' was deactivated automatically because this datasource was deactivated",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                + " to 'true' and configure datasource '<default>'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
