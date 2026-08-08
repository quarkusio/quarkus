package io.quarkus.arc.test.eager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Eager;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

// this test relies on `ArcTestContainer` _not_ firing `Startup`!
public class EagerApplicationScopedStereotypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyStereotype.class, MyBean1.class, MyProducerField.class, MyProducerMethod.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean4.class)
                            .types(MyBean4.class)
                            .stereotypes(context.get(Key.STEREOTYPES).get(DotName.createSimple(MyStereotype.class)))
                            .creator(MyBean4Creator.class)
                            .done();
                }
            })
            .build();

    @Test
    public void test() {
        assertFalse(MyBean1.constructed);
        assertFalse(MyBean2.constructed);
        assertFalse(MyBean3.constructed);
        assertFalse(MyBean4.constructed);

        Arc.container().beanManager().getEvent().fire(new Startup());

        assertTrue(MyBean1.constructed);
        assertTrue(MyBean2.constructed);
        assertTrue(MyBean3.constructed);
        assertTrue(MyBean4.constructed);
    }

    @Eager
    @ApplicationScoped
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @interface MyStereotype {
    }

    @MyStereotype
    static class MyBean1 {
        static boolean constructed = false;

        @PostConstruct
        void init() {
            constructed = true;
        }
    }

    static class MyBean2 {
        static boolean constructed = false;

        public MyBean2() {
        }

        public MyBean2(int unused) {
            constructed = true;
        }
    }

    static class MyBean3 {
        static boolean constructed = false;

        public MyBean3() {
        }

        public MyBean3(int unused) {
            constructed = true;
        }
    }

    static class MyBean4 {
        static boolean constructed = false;

        public MyBean4() {
        }

        public MyBean4(int unused) {
            constructed = true;
        }
    }

    @Dependent
    static class MyProducerField {
        @MyStereotype
        @Produces
        MyBean2 produce = new MyBean2(0);
    }

    @Dependent
    static class MyProducerMethod {
        @MyStereotype
        @Produces
        MyBean3 produce() {
            return new MyBean3(0);
        }
    }

    static class MyBean4Creator implements BeanCreator<MyBean4> {
        @Override
        public MyBean4 create(SyntheticCreationalContext<MyBean4> context) {
            return new MyBean4(0);
        }
    }
}
