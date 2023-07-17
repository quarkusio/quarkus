package io.quarkus.arc.test.producer.disposer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class StaticDisposerTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Producers.class, Dependency.class);

    @Test
    public void testProducersDisposers() {
        assertState(0, 0, false, false, 0, 0);

        InstanceHandle<BigInteger> intHandle = Arc.container().instance(BigInteger.class);
        assertEquals(1, intHandle.get().intValue()); // instance producer
        assertState(1, 1, false, false, 1, 0);
        intHandle.destroy(); // static disposer
        assertState(1, 1, false, true, 2, 2);

        InstanceHandle<BigDecimal> decHandle = Arc.container().instance(BigDecimal.class);
        assertEquals(2, decHandle.get().intValue()); // static producer
        assertState(1, 1, false, true, 3, 2);
        decHandle.destroy(); // instance disposer
        assertState(2, 2, true, true, 4, 4);
    }

    private void assertState(int expectedInstancesCreated, int expectedInstancesDestroyed,
            boolean expectedInstanceDisposerCalled, boolean expectedStaticDisposerCalled,
            int expectedDependencyCreated, int expectedDependencyDestroyed) {
        assertEquals(expectedInstancesCreated, Producers.instancesCreated);
        assertEquals(expectedInstancesDestroyed, Producers.instancesDestroyed);
        assertEquals(expectedInstanceDisposerCalled, Producers.instanceDisposerCalled);
        assertEquals(expectedStaticDisposerCalled, Producers.staticDisposerCalled);
        assertEquals(expectedDependencyCreated, Dependency.created);
        assertEquals(expectedDependencyDestroyed, Dependency.destroyed);
    }

    @Dependent
    static class Producers {
        static int instancesCreated = 0;
        static int instancesDestroyed = 0;

        static boolean instanceDisposerCalled = false;
        static boolean staticDisposerCalled = false;

        @PostConstruct
        void created() {
            instancesCreated++;
        }

        @PreDestroy
        void destroy() {
            instancesDestroyed++;
        }

        @Produces
        @Dependent
        BigInteger instanceProducer(Dependency ignored) {
            return new BigInteger("1");
        }

        @Produces
        @Dependent
        static BigDecimal staticProducer(Dependency ignored) {
            return new BigDecimal("2.0");
        }

        void instanceDisposer(@Disposes BigDecimal disposed, Dependency ignored) {
            assertFalse(instanceDisposerCalled);
            assertEquals(2, disposed.intValue());
            instanceDisposerCalled = true;
        }

        static void staticDisposer(@Disposes BigInteger disposed, Dependency ignored) {
            assertFalse(staticDisposerCalled);
            assertEquals(1, disposed.intValue());
            staticDisposerCalled = true;
        }
    }

    @Dependent
    static class Dependency {
        static int created = 0;
        static int destroyed = 0;

        @PostConstruct
        void created() {
            created++;
        }

        @PreDestroy
        void destroyed() {
            destroyed++;
        }
    }
}
