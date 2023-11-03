package io.quarkus.smallrye.context.deployment.test.cdi;

import static io.quarkus.smallrye.context.deployment.test.cdi.Utils.providersToStringSet;
import static io.quarkus.smallrye.context.deployment.test.cdi.Utils.unwrapExecutor;
import static io.quarkus.smallrye.context.deployment.test.cdi.Utils.unwrapThreadContext;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;
import io.smallrye.context.impl.ThreadContextProviderPlan;

/**
 * Tests that @ManagedExecutorConfig/@ThreadContextConfig and @NamedInstance can be used and injected
 */
public class ConfiguredAndSharedBeansTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Utils.class));

    @Inject
    SomeBean bean;

    @Test
    public void test() {
        bean.ping();

        // firstly, assert that all beans are injectable with their respective names
        Assertions.assertTrue(Arc.container().select(ManagedExecutor.class, NamedInstance.Literal.of(
                SomeBean.class.getName() + "/configuredExecutor"))
                .isResolvable());
        Assertions.assertTrue(Arc.container().select(ManagedExecutor.class, NamedInstance.Literal.of("sharedDefaultExecutor"))
                .isResolvable());
        Assertions.assertTrue(Arc.container()
                .select(ManagedExecutor.class, NamedInstance.Literal.of("sharedConfiguredExecutor")).isResolvable());
        Assertions.assertTrue(Arc.container().select(ThreadContext.class, NamedInstance.Literal.of(
                SomeBean.class.getName() + "/configuredThreadContext"))
                .isResolvable());
        Assertions.assertTrue(Arc.container()
                .select(ThreadContext.class, NamedInstance.Literal.of("sharedDefaultThreadContext")).isResolvable());
        Assertions.assertTrue(Arc.container()
                .select(ThreadContext.class, NamedInstance.Literal.of("sharedConfiguredThreadContext")).isResolvable());

        bean.assertDefaultExecutor();
        bean.assertConfiguredManagedExecutor();
        bean.assertSharedExecutorsAreTheSame();

        bean.assertDefaultThreadContext();
        bean.assertConfiguredThreadContext();
        bean.assertSharedThreadContextsAreTheSame();

        bean.assertUserDefinedProducersAreRespected();
    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier {
    }

    //no proxy bean just so that we can access fields directly
    @Singleton
    static class SomeBean {

        @Inject
        @ManagedExecutorConfig
        ManagedExecutor defaultExecutor;
        @Inject
        @ManagedExecutorConfig(maxAsync = 2, maxQueued = 3)
        ManagedExecutor configuredExecutor;
        @Inject
        @NamedInstance("sharedDefaultExecutor")
        ManagedExecutor sharedDefaultExecutor1;
        @Inject
        @NamedInstance("sharedDefaultExecutor")
        ManagedExecutor sharedDefaultExecutor2;
        @Inject
        @NamedInstance("sharedConfiguredExecutor")
        ManagedExecutor sharedConfiguredExecutor1;
        @Inject
        @ManagedExecutorConfig(maxQueued = 2, maxAsync = 2, cleared = "CDI")
        @NamedInstance("sharedConfiguredExecutor")
        ManagedExecutor sharedConfiguredExecutor2;
        @Inject
        @ThreadContextConfig
        ThreadContext defaultThreadContext;
        @Inject
        @ThreadContextConfig(cleared = "CDI")
        ThreadContext configuredThreadContext;
        @Inject
        @NamedInstance("sharedDefaultThreadContext")
        ThreadContext sharedDefaultThreadContext1;
        @Inject
        @NamedInstance("sharedDefaultThreadContext")
        ThreadContext sharedDefaultThreadContext2;
        @Inject
        @ThreadContextConfig(cleared = "CDI", unchanged = "Remaining", propagated = "")
        @NamedInstance("sharedConfiguredThreadContext")
        ThreadContext sharedConfiguredThreadContext1;
        @Inject
        @NamedInstance("sharedConfiguredThreadContext")
        ThreadContext sharedConfiguredThreadContext2;
        @Inject
        @NamedInstance("userProduced")
        ManagedExecutor userProducedME;
        @Inject
        @NamedInstance("userProduced")
        ThreadContext userProducedTC;
        @Inject
        @MyQualifier
        ManagedExecutor userProducedMEWithQualifier;
        @Inject
        @MyQualifier
        ThreadContext userProducedTCWithQualifier;

        public void ping() {
            // noop method just to verify bean injection
        }

        public void assertDefaultExecutor() {
            SmallRyeManagedExecutor exec = unwrapExecutor(defaultExecutor);
            assertEquals(-1, exec.getMaxAsync());
            assertEquals(-1, exec.getMaxQueued());
            ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
            assertEquals(0, plan.unchangedProviders.size());
            assertEquals(0, plan.clearedProviders.size());
            Set<String> propagated = providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.contains(ThreadContext.CDI));
        }

        public void assertSharedExecutorsAreTheSame() {
            // simply unwrap both and compare reference
            SmallRyeManagedExecutor shared1 = unwrapExecutor(sharedConfiguredExecutor1);
            SmallRyeManagedExecutor shared2 = unwrapExecutor(sharedConfiguredExecutor2);
            assertSame(shared1, shared2);

            shared1 = unwrapExecutor(sharedDefaultExecutor1);
            shared2 = unwrapExecutor(sharedDefaultExecutor2);
            assertSame(shared1, shared2);
        }

        public void assertConfiguredManagedExecutor() {
            SmallRyeManagedExecutor exec = unwrapExecutor(configuredExecutor);
            assertEquals(2, exec.getMaxAsync());
            assertEquals(3, exec.getMaxQueued());
            ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
            assertEquals(0, plan.unchangedProviders.size());
            Set<String> propagated = providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.contains(ThreadContext.CDI));
        }

        public void assertSharedThreadContextsAreTheSame() {
            //unwrap and compare references
            SmallRyeThreadContext shared1 = unwrapThreadContext(sharedDefaultThreadContext1);
            SmallRyeThreadContext shared2 = unwrapThreadContext(sharedDefaultThreadContext2);
            assertSame(shared1, shared2);

            shared1 = unwrapThreadContext(sharedConfiguredThreadContext1);
            shared2 = unwrapThreadContext(sharedConfiguredThreadContext2);
            assertSame(shared1, shared2);
        }

        public void assertConfiguredThreadContext() {
            SmallRyeThreadContext context = unwrapThreadContext(configuredThreadContext);
            ThreadContextProviderPlan plan = context.getPlan();
            assertEquals(0, plan.unchangedProviders.size());
            Set<String> propagated = providersToStringSet(plan.propagatedProviders);
            Set<String> cleared = providersToStringSet(plan.clearedProviders);
            assertTrue(propagated.isEmpty());
            assertTrue(cleared.contains(ThreadContext.CDI));
        }

        public void assertDefaultThreadContext() {
            SmallRyeThreadContext context = unwrapThreadContext(defaultThreadContext);
            ThreadContextProviderPlan plan = context.getPlan();
            assertEquals(0, plan.unchangedProviders.size());
            assertEquals(0, plan.clearedProviders.size());
            Set<String> propagated = providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.contains(ThreadContext.CDI));
        }

        public void assertUserDefinedProducersAreRespected() {
            SmallRyeManagedExecutor userDefinedExec = unwrapExecutor(userProducedME);
            assertEquals(2, userDefinedExec.getMaxAsync());

            userDefinedExec = unwrapExecutor(userProducedMEWithQualifier);
            assertEquals(2, userDefinedExec.getMaxAsync());

            SmallRyeThreadContext userDefinedContext = unwrapThreadContext(userProducedTC);
            ThreadContextProviderPlan plan = userDefinedContext.getPlan();
            assertEquals(0, plan.unchangedProviders.size());
            assertEquals(1, plan.clearedProviders.size());
            Set<String> propagated = providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.isEmpty());

            userDefinedContext = unwrapThreadContext(userProducedTCWithQualifier);
            plan = userDefinedContext.getPlan();
            assertEquals(0, plan.unchangedProviders.size());
            assertEquals(1, plan.clearedProviders.size());
            propagated = providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.isEmpty());
        }
    }

    @ApplicationScoped
    static class ProducerBean {

        @Produces
        @ApplicationScoped
        @NamedInstance("userProduced")
        ManagedExecutor produceEM() {
            //create any non-default
            return ManagedExecutor.builder().maxAsync(2).build();
        }

        @Produces
        @ApplicationScoped
        @NamedInstance("userProduced")
        ThreadContext produceTC() {
            //create any non-default
            return ThreadContext.builder().propagated().build();
        }

        @Produces
        @ApplicationScoped
        @MyQualifier
        ManagedExecutor produceQualifiedEM() {
            //create any non-default
            return ManagedExecutor.builder().maxAsync(2).build();
        }

        @Produces
        @ApplicationScoped
        @MyQualifier
        ThreadContext produceQualifiedTC() {
            //create any non-default
            return ThreadContext.builder().propagated().build();
        }
    }
}
