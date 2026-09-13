package io.quarkus.devservices.postgresql.deployment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.devservices.postgresql.deployment.PostgresqlDevServicesProcessor.QuarkusPostgreSQLContainer;

/**
 * The init scripts are run from {@code containerIsStarted}. The container is never started here: on a fresh container
 * the inherited implementation fails because it needs the running container, which proves it was invoked, while a
 * reused container must skip it altogether.
 */
public class QuarkusPostgreSQLContainerInitScriptsTest {

    @Test
    public void initScriptsAreNotRunOnAReusedContainer() {
        QuarkusPostgreSQLContainer container = container();
        assertDoesNotThrow(() -> container.containerIsStarted(new InspectContainerResponse(), true));
    }

    @Test
    public void initScriptsAreRunOnAFreshContainer() {
        QuarkusPostgreSQLContainer container = container();
        assertThrows(RuntimeException.class, () -> container.containerIsStarted(new InspectContainerResponse(), false));
    }

    private static QuarkusPostgreSQLContainer container() {
        QuarkusPostgreSQLContainer container = new QuarkusPostgreSQLContainer(Optional.of("postgres:17"),
                OptionalInt.empty(), "test-network", false);
        container.withInitScripts("init.sql");
        return container;
    }
}
