package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointMultiQualifiedTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(QualifierA.class, QualifierB.class, MyDependency.class)
            .beanRegistrars(new TestRegistrar())
            .build();

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean);
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface QualifierA {
        class Literal extends AnnotationLiteral<QualifierA> implements QualifierA {
            public static final Literal INSTANCE = new Literal();
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface QualifierB {
        class Literal extends AnnotationLiteral<QualifierB> implements QualifierB {
            public static final Literal INSTANCE = new Literal();
        }
    }

    @Dependent
    @QualifierA
    @QualifierB
    static class MyDependency {
    }

    static class MyBean {
    }

    static class TestRegistrar implements BeanRegistrar {
        @Override
        public void register(RegistrationContext context) {
            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .scope(BuiltinScope.DEPENDENT.getInfo())
                    .addInjectionPoint(ClassType.create(MyDependency.class),
                            AnnotationInstance.builder(QualifierA.class).build(),
                            AnnotationInstance.builder(QualifierB.class).build())
                    .creator(MyBeanCreator.class)
                    .done();
        }

    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            // different order of qualifiers than in the synthetic bean definition
            context.getInjectedReference(MyDependency.class, QualifierB.Literal.INSTANCE, QualifierA.Literal.INSTANCE);
            return new MyBean();
        }
    }
}
