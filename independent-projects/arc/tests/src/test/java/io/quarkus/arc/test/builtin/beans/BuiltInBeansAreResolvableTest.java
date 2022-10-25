package io.quarkus.arc.test.builtin.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;

public class BuiltInBeansAreResolvableTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(DummyBean.class, DummyQualifier.class)
            .build();

    @Test
    public void testBeanManagerBean() {
        // use dynamic resolution to test it
        Instance<Object> instance = Arc.container().beanManager().createInstance();
        // basic BM is resolvable
        assertTrue(instance.select(BeanManager.class).isResolvable());
        // invoke something on the BM
        assertEquals(1,
                instance.select(BeanManager.class).get().getBeans(DummyBean.class, new DummyQualifier.Literal()).size());
        // you shouldn't be able to select BM with qualifiers
        assertFalse(instance.select(BeanManager.class, new DummyQualifier.Literal()).isResolvable());
    }

    @SuppressWarnings({ "serial", "unchecked", "rawtypes" })
    @Test
    public void testEventBean() {
        // use dynamic resolution to test it
        Instance<Object> instance = Arc.container().beanManager().createInstance();
        // event with no qualifier and raw type
        Instance<Event> rawNoQualifier = instance.select(Event.class);
        assertTrue(rawNoQualifier.isResolvable());
        DummyBean.resetCounters();
        rawNoQualifier.get().fire(new Payload());
        assertEquals(1, DummyBean.noQualifiers);
        assertEquals(0, DummyBean.qualifierTimesTriggered);
        assertEquals(1, DummyBean.timesTriggered);

        // event with type and no qualifier
        Instance<Event<Payload>> typedEventNoQualifier = instance.select(new TypeLiteral<Event<Payload>>() {
        });
        assertTrue(typedEventNoQualifier.isResolvable());
        DummyBean.resetCounters();
        typedEventNoQualifier.get().fire(new Payload());
        assertEquals(1, DummyBean.noQualifiers);
        assertEquals(0, DummyBean.qualifierTimesTriggered);
        assertEquals(1, DummyBean.timesTriggered);

        // event with type and qualifier
        Instance<Event<Payload>> typedEventWithQualifier = instance.select(new TypeLiteral<Event<Payload>>() {
        }, new DummyQualifier.Literal());
        assertTrue(typedEventWithQualifier.isResolvable());
        DummyBean.resetCounters();
        typedEventWithQualifier.get().fire(new Payload());
        assertEquals(1, DummyBean.noQualifiers);
        assertEquals(1, DummyBean.qualifierTimesTriggered);
        assertEquals(1, DummyBean.timesTriggered);

    }

    @SuppressWarnings({ "serial", "unchecked" })
    @Test
    public void testInstanceBean() {
        BeanManager bm = Arc.container().beanManager();

        // verify all selections have a backing bean
        Set<Bean<?>> instanceBeans = bm.getBeans(Instance.class);
        assertEquals(1, instanceBeans.size());
        Set<Bean<?>> typedInstanceBeans = bm.getBeans(new TypeLiteral<Instance<DummyBean>>() {
        }.getType());
        assertEquals(1, typedInstanceBeans.size());
        Set<Bean<?>> typedQualifiedInstanceBean = bm.getBeans(new TypeLiteral<Instance<DummyBean>>() {
        }.getType(), new DummyQualifier.Literal());
        assertEquals(1, typedQualifiedInstanceBean.size());

        InjectionPoint dummyIp = new InjectionPoint() {

            @Override
            public Type getType() {
                return new TypeLiteral<Instance<DummyBean>>() {
                }.getType();
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return new HashSet<>();
            }

            @Override
            public Bean<?> getBean() {
                return null;
            }

            @Override
            public Member getMember() {
                return null;
            }

            @Override
            public Annotated getAnnotated() {
                return null;
            }

            @Override
            public boolean isDelegate() {
                return false;
            }

            @Override
            public boolean isTransient() {
                return false;
            }
        };

        InjectionPoint dummyIpWithQualifiers = new InjectionPoint() {

            @Override
            public Type getType() {
                return new TypeLiteral<Instance<DummyBean>>() {
                }.getType();
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return new HashSet<>(Arrays.asList(Any.Literal.INSTANCE, new DummyQualifier.Literal()));
            }

            @Override
            public Bean<?> getBean() {
                return null;
            }

            @Override
            public Member getMember() {
                return null;
            }

            @Override
            public Annotated getAnnotated() {
                return null;
            }

            @Override
            public boolean isDelegate() {
                return false;
            }

            @Override
            public boolean isTransient() {
                return false;
            }
        };

        Instance<DummyBean> dummyInstance = (Instance<DummyBean>) bm.getInjectableReference(dummyIp,
                new CreationalContextImpl<>(null));
        assertFalse(dummyInstance.isResolvable()); // not resolvable, no qualifier
        dummyInstance.select(new DummyQualifier.Literal()).get().ping();

        Instance<DummyBean> dummyInstanceWithQualifier = (Instance<DummyBean>) bm.getInjectableReference(dummyIpWithQualifiers,
                new CreationalContextImpl<>(null));
        assertTrue(dummyInstanceWithQualifier.isResolvable()); // resolvable, qualifier is present
        dummyInstanceWithQualifier.get().ping();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface DummyQualifier {

        @SuppressWarnings("all")
        public static class Literal extends AnnotationLiteral<DummyQualifier> implements DummyQualifier {
        }
    }

    @ApplicationScoped
    @DummyQualifier
    static class DummyBean {

        public static int noQualifiers = 0;
        public static int qualifierTimesTriggered = 0;
        public static int timesTriggered = 0;

        public void observeEvent(@Observes Payload payload) {
            noQualifiers++;
        }

        public void observeQualifiedEvent(@Observes @DummyQualifier Payload payload) {
            qualifierTimesTriggered++;
        }

        public void observeBasicEvent(@Observes @Any Payload payload) {
            timesTriggered++;
        }

        public static void resetCounters() {
            noQualifiers = 0;
            qualifierTimesTriggered = 0;
            timesTriggered = 0;
        }

        public void ping() {
        }

        ;
    }

    static class Payload {
    }
}
