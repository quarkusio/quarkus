package io.quarkus.devservices.postgresql.deployment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.ext.ScriptUtils;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * The init scripts are run from {@code runInitScriptIfRequired}, which {@code JdbcDatabaseContainer} calls from
 * {@code containerIsStarted} right after logging the JDBC URL. The container is never started here: the script does
 * not exist, so reaching the inherited implementation fails while loading it, and a reused container must skip it
 * altogether and still reach the inherited logging.
 */
public class QuarkusPostgreSQLContainerInitScriptsTest {

    private static final String INIT_SCRIPT = "does-not-exist.sql";

    @Test
    public void initScriptsAreNotRunOnAReusedContainer() {
        TestContainer container = container();
        assertDoesNotThrow(() -> container.containerIsStarted(new InspectContainerResponse(), true));
    }

    @Test
    public void initScriptsAreRunOnAFreshContainer() {
        TestContainer container = container();
        ScriptUtils.ScriptLoadException e = assertThrows(ScriptUtils.ScriptLoadException.class,
                () -> container.containerIsStarted(new InspectContainerResponse(), false));
        assertTrue(e.getMessage().contains(INIT_SCRIPT), e.getMessage());
    }

    @Test
    public void theInheritedStartupLoggingIsKeptOnAReusedContainer() {
        TestContainer container = container();
        container.containerIsStarted(new InspectContainerResponse(), true);
        assertTrue(container.jdbcUrlRequested);
    }

    private static TestContainer container() {
        TestContainer container = new TestContainer();
        container.withInitScripts(INIT_SCRIPT);
        return container;
    }

    /**
     * Reports whether the inherited {@code Container is started (JDBC URL: {})} logging was reached. The URL is taken
     * out of the mapped port that an unstarted container cannot provide, and the logger out of the image name that
     * cannot be resolved without a container runtime.
     */
    private static class TestContainer extends PostgresqlDevServicesProcessor.QuarkusPostgreSQLContainer {

        private boolean jdbcUrlRequested;

        TestContainer() {
            super(Optional.of("postgres:17"), OptionalInt.empty(), "test-network", false);
        }

        @Override
        public String getJdbcUrl() {
            jdbcUrlRequested = true;
            return "jdbc:postgresql://localhost:5432/test";
        }

        @Override
        protected Logger logger() {
            return LoggerFactory.getLogger(TestContainer.class);
        }
    }
}
