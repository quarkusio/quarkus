package io.quarkus.grpc.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.example.security.SecuredService;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityEventsValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(SecurityEventObserver.class)
                    .addPackage(SecuredService.class.getPackage()))
            .assertException(throwable -> {
                assertTrue(throwable instanceof ConfigurationException);
                assertTrue(throwable.getMessage().contains("quarkus.security.events.enabled"));
            });

    @Test
    void test() {
        // must be here to run test
        Assertions.fail();
    }
}
