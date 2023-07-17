package io.quarkus.smallrye.context.deployment.test;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class AllUnchangedConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("mp.context.ThreadContext.unchanged", "Remaining")
            .overrideConfigKey("mp.context.ThreadContext.propagated", "");

    @Inject
    SmallRyeThreadContext ctx;

    @Test
    public void test() {
        ThreadContextProviderPlan plan = ctx.getPlan();
        Assertions.assertTrue(plan.propagatedProviders.isEmpty());
        Assertions.assertTrue(plan.clearedProviders.isEmpty());
        Assertions.assertFalse(plan.unchangedProviders.isEmpty());
    }
}
