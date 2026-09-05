package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.Dependent;
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

public class SyntheticBeanWithMultiQualifierInjectionTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependentBean.class, QualifierA.class, QualifierB.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        MyPojo bean = Arc.container().select(MyPojo.class).get();
        assertNotNull(bean);
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface QualifierA {
        final class Literal extends AnnotationLiteral<QualifierA> implements QualifierA {
            public static final Literal INSTANCE = new Literal();
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface QualifierB {
        final class Literal extends AnnotationLiteral<QualifierB> implements QualifierB {
            public static final Literal INSTANCE = new Literal();
        }
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesise(SyntheticComponents syn) {
            syn.addBean(MyPojo.class)
                    .type(MyPojo.class)
                    .scope(Dependent.class)
                    .withInjectionPoint(MyDependentBean.class, QualifierA.Literal.INSTANCE, QualifierB.Literal.INSTANCE)
                    .createWith(MyPojoCreator.class);
        }
    }

    @Dependent
    @QualifierA
    @QualifierB
    static class MyDependentBean {
    }

    static class MyPojo {
    }

    static class MyPojoCreator implements SyntheticBeanCreator<MyPojo> {
        @Override
        public MyPojo create(SyntheticInjections injections, Parameters params) {
            // different order of qualifiers than in the synthetic bean definition
            injections.get(MyDependentBean.class, QualifierB.Literal.INSTANCE, QualifierA.Literal.INSTANCE);
            return new MyPojo();
        }
    }
}
