package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class TransactionJdbcObjectStoreValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource("jdbc-object-store-validation.properties", "application.properties"))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-jdbc-h2", Version.getVersion())))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                if (rootCause instanceof ConfigurationException) {
                    assertTrue(rootCause.getMessage().contains(
                            "The Narayana JTA extension is configured to use the datasource 'test' but that datasource is not configured."));
                } else {
                    fail(t);
                }
            });

    @Test
    public void test() {
        // needs to be there in order to run test
        Assertions.fail("Application was supposed to fail.");
    }
}
