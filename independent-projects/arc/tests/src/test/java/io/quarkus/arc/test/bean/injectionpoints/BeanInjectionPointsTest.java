package io.quarkus.arc.test.bean.injectionpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class BeanInjectionPointsTest {
    @RegisterExtension
    private ArcTestContainer container = new ArcTestContainer.Builder()
            .beanClasses(MyBean.class, MyDependency.class, MyQualifier1.class, MyQualifier2.class)
            .strictCompatibility(true) // we don't support `Bean.getInjectionPoints()` by default
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MySyntheticBean.class)
                            .types(MySyntheticBean.class)
                            .addInjectionPoint(ClassType.create(MyDependency.class))
                            .creator(MySyntheticBeanCreator.class)
                            .done();
                }
            })
            .build();

    @Test
    public void classBean() {
        InjectableBean<MyBean> bean = Arc.container().instance(MyBean.class).getBean();
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        assertEquals(3, injectionPoints.size());
        for (InjectionPoint injectionPoint : injectionPoints) {
            Member member = injectionPoint.getMember();
            Annotated annotated = injectionPoint.getAnnotated();
            if (member instanceof Field) {
                assertEquals(MyDependency.class, injectionPoint.getType());
                assertEquals(Set.of(Default.Literal.INSTANCE), injectionPoint.getQualifiers());
                assertEquals(bean, injectionPoint.getBean());
                assertEquals("fieldInjection", member.getName());
                assertInstanceOf(AnnotatedField.class, annotated);
                verifyExtraAnnotation(annotated, "a");
            } else if (member instanceof Constructor) {
                assertEquals("io.quarkus.arc.test.bean.injectionpoints.BeanInjectionPointsTest$MyBean", member.getName());
                assertEquals(MyDependency.class, injectionPoint.getType());
                assertEquals(Set.of(MyQualifier1.Literal.INSTANCE), injectionPoint.getQualifiers());
                assertEquals(bean, injectionPoint.getBean());
                assertInstanceOf(AnnotatedParameter.class, annotated);
                assertEquals(0, ((AnnotatedParameter<?>) annotated).getPosition());
                verifyExtraAnnotation(annotated, "b");
            } else if (member instanceof Method) {
                assertEquals("init", member.getName());
                assertEquals(MyDependency.class, injectionPoint.getType());
                assertEquals(Set.of(MyQualifier1.Literal.INSTANCE, MyQualifier2.Literal.INSTANCE),
                        injectionPoint.getQualifiers());
                assertEquals(bean, injectionPoint.getBean());
                assertInstanceOf(AnnotatedParameter.class, annotated);
                assertEquals(0, ((AnnotatedParameter<?>) annotated).getPosition());
                verifyExtraAnnotation(annotated, "c");
            } else {
                fail("Unknown injection point: " + injectionPoint);
            }
        }
    }

    @Test
    public void producerMethodBean() {
        assertTrue(Arc.container().instance(MyDependency.class).getBean().getInjectionPoints().isEmpty());

        InjectableBean<String> bean = Arc.container().instance(String.class).getBean();
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        assertEquals(1, injectionPoints.size());
        InjectionPoint injectionPoint = injectionPoints.iterator().next();
        assertEquals(MyDependency.class, injectionPoint.getType());
        assertEquals(Set.of(MyQualifier2.Literal.INSTANCE), injectionPoint.getQualifiers());
        assertEquals(bean, injectionPoint.getBean());
        assertEquals("produce", injectionPoint.getMember().getName());
        assertInstanceOf(AnnotatedParameter.class, injectionPoint.getAnnotated());
        assertEquals(0, ((AnnotatedParameter<?>) injectionPoint.getAnnotated()).getPosition());
        verifyExtraAnnotation(injectionPoint.getAnnotated(), "f");
    }

    @Test
    public void syntheticBean() {
        InjectableBean<MySyntheticBean> bean = Arc.container().instance(MySyntheticBean.class).getBean();
        Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();
        assertEquals(1, injectionPoints.size());
        InjectionPoint injectionPoint = injectionPoints.iterator().next();
        assertEquals(MyDependency.class, injectionPoint.getType());
        assertEquals(Set.of(Default.Literal.INSTANCE), injectionPoint.getQualifiers());
        assertEquals(bean, injectionPoint.getBean());
        assertNull(injectionPoint.getMember());
        assertNull(injectionPoint.getAnnotated());
    }

    private void verifyExtraAnnotation(Annotated annotated, String expectedValue) {
        assertTrue(annotated.isAnnotationPresent(ExtraAnnotation.class));

        assertNotNull(annotated.getAnnotation(ExtraAnnotation.class));
        assertEquals(expectedValue, annotated.getAnnotation(ExtraAnnotation.class).value());

        assertNotNull(annotated.getAnnotations(ExtraAnnotation.class));
        assertEquals(1, annotated.getAnnotations(ExtraAnnotation.class).size());
        assertEquals(expectedValue, annotated.getAnnotations(ExtraAnnotation.class).iterator().next().value());

        Set<Annotation> all = annotated.getAnnotations();
        assertNotNull(all);
        assertTrue(all.size() >= 1);
        boolean found = false;
        for (Annotation annotation : all) {
            if (annotation instanceof ExtraAnnotation) {
                found = true;
                assertEquals(expectedValue, ((ExtraAnnotation) annotation).value());
            }
        }
        assertTrue(found);
    }

    @Singleton
    static class MyBean {
        @Inject
        @ExtraAnnotation("a")
        MyDependency fieldInjection;

        @Inject
        MyBean(@MyQualifier1 @ExtraAnnotation("b") MyDependency constructorInjection) {
        }

        @Inject
        void init(@MyQualifier1 @MyQualifier2 @ExtraAnnotation("c") MyDependency initializerMethodInjection) {
        }

        void observe(@Observes String ignored, @ExtraAnnotation("d") MyDependency observerMethodInjection) {
        }

        void observeAsync(@ObservesAsync String ignored, @ExtraAnnotation("e") MyDependency asyncObserverMethodInjection) {
        }

        @Produces
        String produce(@MyQualifier2 @ExtraAnnotation("f") MyDependency producerMethodInjection) {
            return "";
        }

        void dispose(@Disposes String ignored, @ExtraAnnotation("g") MyDependency disposerMethodInjection) {
        }
    }

    @Dependent
    @Default
    @MyQualifier1
    @MyQualifier2
    static class MyDependency {
    }

    static class MySyntheticBean {
    }

    static class MySyntheticBeanCreator implements BeanCreator<MySyntheticBean> {
        @Override
        public MySyntheticBean create(SyntheticCreationalContext<MySyntheticBean> context) {
            return new MySyntheticBean();
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier1 {
        final class Literal extends AnnotationLiteral<MyQualifier1> implements MyQualifier1 {
            static final Literal INSTANCE = new Literal();
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier2 {
        final class Literal extends AnnotationLiteral<MyQualifier2> implements MyQualifier2 {
            static final Literal INSTANCE = new Literal();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ExtraAnnotation {
        String value();
    }
}
