package io.quarkus.hibernate.orm.multiplepersistenceunits;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsBlockingModeReactiveOnlyDatasourceTest {

    static final String QUARKUS_VERSION = System.getProperty("project.version", "999-SNAPSHOT");

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-pg-client", QUARKUS_VERSION)))
            .assertException(t -> {
                Assertions.assertInstanceOf(ConfigurationException.class, t);
                String msg = t.getMessage();
                Assertions.assertTrue(
                        msg.contains("mode=BLOCKING")
                                && msg.contains("no JDBC datasource")
                                && msg.contains("set mode="),
                        "Unexpected exception message: " + msg);
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-multiple-persistence-units-mode-blocking-reactive-only.properties",
                            "application.properties"));

    @Test
    public void shouldFailAtBuildTime() {
        Assertions.fail();
    }
}