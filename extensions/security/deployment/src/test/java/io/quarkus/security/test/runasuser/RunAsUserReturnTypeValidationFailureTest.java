package io.quarkus.security.test.runasuser;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.RunAsUser;
import io.quarkus.security.spi.RunAsUserPredicateBuildItem;
import io.quarkus.test.QuarkusUnitTest;

class RunAsUserReturnTypeValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(FakeScheduled.class))
            .addBuildChainCustomizer(b -> b
                    .addBuildStep(context -> context.produce(RunAsUserPredicateBuildItem.ofAnnotation(FakeScheduled.class)))
                    .produces(RunAsUserPredicateBuildItem.class).build())
            .assertException(t -> {
                assertInstanceOf(RuntimeException.class, t);
                String exceptionMessage = t.getMessage();
                assertNotNull(exceptionMessage);
                String expectedFailure1 = "return type other than";
                assertTrue(exceptionMessage.contains(expectedFailure1),
                        () -> "Expected failure message to contain '%s', but got: %s".formatted(expectedFailure1,
                                exceptionMessage));
                String expectedFailure2 = "RunAsUserReturnTypeValidationFailureTest#wrongReturnType";
                assertTrue(exceptionMessage.contains(expectedFailure2),
                        () -> "Expected failure message to contain '%s', but got: %s".formatted(expectedFailure2,
                                exceptionMessage));
            });

    @Test
    void runTest() {
        fail("Expected validation failure.");
    }

    @FakeScheduled
    @RunAsUser(user = "ignored")
    Object wrongReturnType() {
        return null;
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface FakeScheduled {
    }
}
