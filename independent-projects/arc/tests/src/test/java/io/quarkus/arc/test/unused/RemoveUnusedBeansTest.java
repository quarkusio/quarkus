package io.quarkus.arc.test.unused;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.arc.test.ArcTestContainer;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RemoveUnusedBeansTest extends RemoveUnusedComponentsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(HasObserver.class, Foo.class, FooAlternative.class, HasName.class, UnusedProducers.class,
                    InjectedViaInstance.class, InjectedViaInstanceWithWildcard.class, InjectedViaInstanceWithWildcard2.class,
                    InjectedViaProvider.class, Excluded.class,
                    UsedProducers.class,
                    UnusedProducerButInjected.class, UsedViaInstanceWithUnusedProducer.class, UsesBeanViaInstance.class)
            .removeUnusedBeans(true)
            .addRemovalExclusion(b -> b.getBeanClass().toString().equals(Excluded.class.getName()))
            .build();

    @Test
    public void testRemoval() {
        assertPresent(HasObserver.class);
        assertPresent(HasName.class);
        assertPresent(InjectedViaInstance.class);
        assertPresent(InjectedViaInstanceWithWildcard.class);
        assertPresent(InjectedViaInstanceWithWildcard2.class);
        assertPresent(InjectedViaProvider.class);
        assertPresent(String.class);
        assertPresent(UsedProducers.class);
        assertNotPresent(UnusedProducers.class);
        assertNotPresent(BigDecimal.class);
        ArcContainer container = Arc.container();
        // Foo is injected in HasObserver#observe()
        Foo foo = container.instance(Foo.class).get();
        assertEquals(FooAlternative.class.getName(), foo.ping());
        assertTrue(foo.provider.get().isValid());
        assertEquals(1, container.beanManager().getBeans(Foo.class).size());
        assertEquals("pong", container.instance(Excluded.class).get().ping());
        // Producer is unused but declaring bean is injected
        assertPresent(UnusedProducerButInjected.class);
        assertNotPresent(BigInteger.class);
        // Producer is unused, declaring bean is only used via Instance
        assertPresent(UsedViaInstanceWithUnusedProducer.class);
        assertNotPresent(Long.class);
        assertFalse(ArcContainerImpl.instance().getRemovedBeans().isEmpty());
        assertNotPresent(UnusedBean.class);
        assertNotPresent(OnlyInjectedInUnusedBean.class);
    }

    @Dependent
    static class HasObserver {

        void observe(@Observes String event, Foo foo) {
        }

    }

    @Named
    @Dependent
    static class HasName {

    }

    @Dependent
    static class Foo {

        @Inject
        Provider<InjectedViaProvider> provider;

        @Inject
        UnusedProducerButInjected injected;

        String ping() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(1)
    @Dependent
    static class FooAlternative extends Foo {

        @Inject
        Instance<InjectedViaInstance> instance;

        @Inject
        Instance<? extends InjectedViaInstanceWithWildcard> instanceWildcard;

        @Inject
        Instance<Comparable<? extends Foo>> instanceWildcard2;

        @Inject
        String foo;

    }

    @Singleton
    static class InjectedViaInstance {

    }

    @Singleton
    static class InjectedViaInstanceWithWildcard {

    }

    @Singleton
    static class InjectedViaInstanceWithWildcard2 implements Comparable<FooAlternative> {

        @Override
        public int compareTo(FooAlternative o) {
            return 0;
        }

    }

    @Singleton
    static class InjectedViaProvider {

        private boolean isValid;

        @PostConstruct
        void init() {
            isValid = true;
        }

        boolean isValid() {
            return isValid;
        }

    }

    @Singleton
    static class UnusedProducers {

        @Produces
        BigDecimal unusedNumber() {
            return BigDecimal.ZERO;
        }

    }

    @Singleton
    static class UsedProducers {

        @Produces
        String usedString() {
            return "ok";
        }

    }

    @Singleton
    static class Excluded {

        String ping() {
            return "pong";
        }

    }

    @Singleton
    static class UnusedProducerButInjected {

        @Produces
        BigInteger unusedNumber() {
            return BigInteger.ZERO;
        }

    }

    @Singleton
    static class UsedViaInstanceWithUnusedProducer {

        @Produces
        Long unusedLong = Long.valueOf(0);
    }

    @Named // just to make it unremovable
    @Singleton
    static class UsesBeanViaInstance {

        @Inject
        Instance<UsedViaInstanceWithUnusedProducer> instance;
    }

    @Singleton
    static class UnusedBean {

        @Inject
        OnlyInjectedInUnusedBean beanB;

    }

    @Singleton
    static class OnlyInjectedInUnusedBean {

    }

}
