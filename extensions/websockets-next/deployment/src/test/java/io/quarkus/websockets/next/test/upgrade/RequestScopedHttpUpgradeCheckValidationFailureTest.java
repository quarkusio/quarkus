package io.quarkus.websockets.next.test.upgrade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.RequestScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;

public class RequestScopedHttpUpgradeCheckValidationFailureTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(RequestScopedHttpUpgradeCheck.class))
            .assertException(t -> {
                assertTrue(t.getMessage().contains("RequestScopedHttpUpgradeCheck"), t.getMessage());
                assertTrue(t.getMessage().contains("jakarta.enterprise.context.RequestScoped"), t.getMessage());
                assertTrue(t.getMessage().contains(
                        "but the '%s' implementors must be one either `@ApplicationScoped', '@Singleton' or '@Dependent' beans"
                                .formatted(HttpUpgradeCheck.class.getName())),
                        t.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @RequestScoped
    public static class RequestScopedHttpUpgradeCheck implements HttpUpgradeCheck {

        @Override
        public Uni<CheckResult> perform(HttpUpgradeContext context) {
            return CheckResult.permitUpgrade();
        }
    }
}
