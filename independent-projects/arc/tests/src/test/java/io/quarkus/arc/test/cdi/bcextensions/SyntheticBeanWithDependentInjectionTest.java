package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticInjections;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticBeanWithDependentInjectionTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyProducer.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyPojo bean = Arc.container().select(MyPojo.class).get();
        assertNotNull(bean.dependency);
        assertNull(bean.qualifiedDependency);
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesise(SyntheticComponents syn) {
            syn.addBean(MyPojo.class)
                    .type(MyPojo.class)
                    .scope(Dependent.class)
                    .withInjectionPoint(MyDependency.class)
                    .withInjectionPoint(MyDependency.class, MyQualifier.Literal.INSTANCE)
                    .createWith(MyPojoCreator.class);
        }
    }

    // ---

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyQualifier {
        final class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {
            public static final Literal INSTANCE = new Literal();
        }
    }

    static class MyDependency {
    }

    @Dependent
    static class MyProducer {
        @Produces
        @Dependent
        MyDependency produce() {
            return new MyDependency();
        }

        @Produces
        @Dependent
        @MyQualifier
        MyDependency produceQualified() {
            return null;
        }
    }

    static class MyPojo {
        final MyDependency dependency;
        final MyDependency qualifiedDependency;

        MyPojo(MyDependency dependency, MyDependency qualifiedDependency) {
            this.dependency = dependency;
            this.qualifiedDependency = qualifiedDependency;
        }
    }

    static class MyPojoCreator implements SyntheticBeanCreator<MyPojo> {
        @Override
        public MyPojo create(SyntheticInjections injections, Parameters params) {
            return new MyPojo(
                    injections.get(MyDependency.class),
                    injections.get(MyDependency.class, MyQualifier.Literal.INSTANCE));
        }
    }
}
