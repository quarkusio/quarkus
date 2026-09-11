package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
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

public class SyntheticInjectionPointDependentTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyQualifier.class, MyProducer.class)
            .beanRegistrars(new TestRegistrar())
            .build();

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertNotNull(bean.dep);
        assertNull(bean.qualifiedDep);
    }

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

    static class MyBean {
        final MyDependency dep;
        final MyDependency qualifiedDep;

        MyBean(MyDependency dep, MyDependency qualifiedDep) {
            this.dep = dep;
            this.qualifiedDep = qualifiedDep;
        }
    }

    static class TestRegistrar implements BeanRegistrar {
        @Override
        public void register(RegistrationContext context) {
            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .scope(BuiltinScope.DEPENDENT.getInfo())
                    .addInjectionPoint(ClassType.create(MyDependency.class))
                    .addInjectionPoint(ClassType.create(MyDependency.class),
                            AnnotationInstance.builder(MyQualifier.class).build())
                    .creator(MyBeanCreator.class)
                    .done();
        }

    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean(
                    context.getInjectedReference(MyDependency.class),
                    context.getInjectedReference(MyDependency.class, MyQualifier.Literal.INSTANCE));
        }
    }
}
