package io.quarkus.security.test.runasuser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.RunAsUser;
import io.quarkus.test.QuarkusUnitTest;

class RunAsUserMissingAnnotationValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .assertException(t -> {
                assertInstanceOf(RuntimeException.class, t);
                String exceptionMessage = t.getMessage();
                assertNotNull(exceptionMessage);
                String expectedFailure1 = "cannot be used on following methods";
                assertTrue(exceptionMessage.contains(expectedFailure1),
                        () -> "Expected failure message to contain '%s', but got: %s".formatted(expectedFailure1,
                                exceptionMessage));
                String expectedFailure2 = "RunAsUserMissingAnnotationValidationFailureTest#runTest";
                assertTrue(exceptionMessage.contains(expectedFailure2),
                        () -> "Expected failure message to contain '%s', but got: %s".formatted(expectedFailure2,
                                exceptionMessage));
            });

    @RunAsUser(user = "ignored")
    @Test
    void runTest() {
        fail("Expected validation failure.");
    }

}
