package io.quarkus.arc.test.builtin.beans;

import io.quarkus.arc.Arc;
import io.quarkus.arc.CreationalContextImpl;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Qualifier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class BuiltInBeansAreResolvableTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(DummyBean.class, DummyQualifier.class).build();

    @Test
    public void testBeanManagerBean() {
        // use dynamic resolution to test it
        Instance<Object> instance = Arc.container().beanManager().createInstance();
        // basic BM is resolvable
        Assert.assertTrue(instance.select(BeanManager.class).isResolvable());
        // invoke something on the BM
        Assert.assertEquals(1, instance.select(BeanManager.class).get().getBeans(DummyBean.class).size());
        // you shouldn't be able to select BM with qualifiers
        Assert.assertFalse(instance.select(BeanManager.class, new DummyQualifier.Literal()).isResolvable());
    }

    @Test
    public void testEventBean() {
        // use dynamic resolution to test it
        Instance<Object> instance = Arc.container().beanManager().createInstance();
        // event with no qualifier and raw type
        Instance<Event> rawNoQualifier = instance.select(Event.class);
        Assert.assertTrue(rawNoQualifier.isResolvable());
        DummyBean.resetCounters();
        rawNoQualifier.get().fire(new Object());
        Assert.assertEquals(0, DummyBean.noQualifiers);
        Assert.assertEquals(0, DummyBean.qualifierTimesTriggered);
        Assert.assertEquals(1, DummyBean.objectTimesTriggered);

        // event with type and no qualifier
        Instance<Event<String>> typedEventNoQualifier = instance.select(new TypeLiteral<Event<String>>() {
        });
        Assert.assertTrue(typedEventNoQualifier.isResolvable());
        DummyBean.resetCounters();
        typedEventNoQualifier.get().fire("foo");
        Assert.assertEquals(1, DummyBean.noQualifiers);
        Assert.assertEquals(0, DummyBean.qualifierTimesTriggered);
        Assert.assertEquals(1, DummyBean.objectTimesTriggered);

        // event with type and qualifier
        Instance<Event<String>> typedEventWithQualifier = instance.select(new TypeLiteral<Event<String>>() {
        }, new DummyQualifier.Literal());
        Assert.assertTrue(typedEventWithQualifier.isResolvable());
        DummyBean.resetCounters();
        typedEventWithQualifier.get().fire("foo");
        Assert.assertEquals(1, DummyBean.noQualifiers);
        Assert.assertEquals(1, DummyBean.qualifierTimesTriggered);
        Assert.assertEquals(1, DummyBean.objectTimesTriggered);

    }

    @Test
    public void testInstanceBean() {
        BeanManager bm = Arc.container().beanManager();

        // verify all selections have a backing bean
        Set<Bean<?>> instanceBeans = bm.getBeans(Instance.class);
        Assert.assertEquals(1, instanceBeans.size());
        Set<Bean<?>> typedInstanceBeans = bm.getBeans(new TypeLiteral<Instance<DummyBean>>() {
        }.getType());
        Assert.assertEquals(1, typedInstanceBeans.size());
        Set<Bean<?>> typedQualifiedInstanceBean = bm.getBeans(new TypeLiteral<Instance<DummyBean>>() {
        }.getType(), new DummyQualifier.Literal());
        Assert.assertEquals(1, typedQualifiedInstanceBean.size());

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
        Assert.assertFalse(dummyInstance.isResolvable()); // not resolvable, no qualifier
        dummyInstance.select(new DummyQualifier.Literal()).get().ping();

        Instance<DummyBean> dummyInstanceWithQualifier = (Instance<DummyBean>) bm.getInjectableReference(dummyIpWithQualifiers,
                new CreationalContextImpl<>(null));
        Assert.assertTrue(dummyInstanceWithQualifier.isResolvable()); // resolvable, qualifier is present
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
        public static int objectTimesTriggered = 0;

        public void observeEvent(@Observes String payload) {
            noQualifiers++;
        }

        public void observeQualifiedEvent(@Observes @DummyQualifier String payload) {
            qualifierTimesTriggered++;
        }

        public void observeBasicEvent(@Observes @Any Object objPayload) {
            objectTimesTriggered++;
        }

        public static void resetCounters() {
            noQualifiers = 0;
            qualifierTimesTriggered = 0;
            objectTimesTriggered = 0;
        }

        public void ping() {
        };
    }
}
