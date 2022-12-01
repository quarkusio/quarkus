package io.quarkus.smallrye.context.deployment.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class NoConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    SmallRyeThreadContext ctx;

    @Test
    public void test() {
        ThreadContextProviderPlan plan = ctx.getPlan();
        Assertions.assertFalse(plan.propagatedProviders.isEmpty());
        Assertions.assertTrue(plan.clearedProviders.isEmpty());
        Assertions.assertTrue(plan.unchangedProviders.isEmpty());
    }
}
