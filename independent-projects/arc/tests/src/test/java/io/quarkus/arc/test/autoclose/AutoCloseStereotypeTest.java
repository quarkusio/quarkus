package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.AutoClose;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseStereotypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyStereotype.class, MyBean1.class, MyCloseableBean1.class, Producers.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean4.class)
                            .types(MyBean4.class)
                            .stereotypes(context.get(Key.STEREOTYPES).get(DotName.createSimple(MyStereotype.class)))
                            .creator(MyBean4Creator.class)
                            .destroyer(MyBean4Disposer.class)
                            .done();

                    context.configure(MyCloseableBean4.class)
                            .types(MyCloseableBean4.class)
                            .stereotypes(context.get(Key.STEREOTYPES).get(DotName.createSimple(MyStereotype.class)))
                            .creator(MyCloseableBean4Creator.class)
                            .destroyer(MyCloseableBean4Disposer.class)
                            .done();
                }
            })
            .build();

    @Test
    public void bean() {
        {
            Instance<MyBean1> instance = Arc.container().select(MyBean1.class);
            MyBean1 bean = instance.get();
            assertEquals(List.of(), MyBean1.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy"), MyBean1.actions);
        }
        {
            Instance<MyCloseableBean1> instance = Arc.container().select(MyCloseableBean1.class);
            MyCloseableBean1 bean = instance.get();
            assertEquals(List.of(), MyCloseableBean1.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy", "close"), MyCloseableBean1.actions);
        }
    }

    @Test
    public void producerField() {
        {
            Instance<MyBean2> instance = Arc.container().select(MyBean2.class);
            MyBean2 bean = instance.get();
            assertEquals(List.of(), MyBean2.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy"), MyBean2.actions);
        }
        {
            Instance<MyCloseableBean2> instance = Arc.container().select(MyCloseableBean2.class);
            MyCloseableBean2 bean = instance.get();
            assertEquals(List.of(), MyCloseableBean2.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy", "close"), MyCloseableBean2.actions);
        }
    }

    @Test
    public void producerMethod() {
        {
            Instance<MyBean3> instance = Arc.container().select(MyBean3.class);
            MyBean3 bean = instance.get();
            assertEquals(List.of(), MyBean3.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy"), MyBean3.actions);
        }
        {
            Instance<MyCloseableBean3> instance = Arc.container().select(MyCloseableBean3.class);
            MyCloseableBean3 bean = instance.get();
            assertEquals(List.of(), MyCloseableBean3.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy", "close"), MyCloseableBean3.actions);
        }
    }

    @Test
    public void syntheticBean() {
        {
            Instance<MyBean4> instance = Arc.container().select(MyBean4.class);
            MyBean4 bean = instance.get();
            assertEquals(List.of(), MyBean4.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy"), MyBean4.actions);
        }
        {
            Instance<MyCloseableBean4> instance = Arc.container().select(MyCloseableBean4.class);
            MyCloseableBean4 bean = instance.get();
            assertEquals(List.of(), MyCloseableBean4.actions);
            instance.destroy(bean);
            assertEquals(List.of("destroy", "close"), MyCloseableBean4.actions);
        }
    }

    @AutoClose
    @Dependent
    @Stereotype
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @interface MyStereotype {
    }

    @MyStereotype
    static class MyBean1 {
        static final List<String> actions = new ArrayList<>();

        @PreDestroy
        public void destroy() {
            actions.add("destroy");
        }
    }

    @MyStereotype
    static class MyCloseableBean1 implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @PreDestroy
        public void destroy() {
            actions.add("destroy");
        }

        @Override
        public void close() {
            actions.add("close");
        }
    }

    @Singleton
    static class Producers {
        @Produces
        @MyStereotype
        MyBean2 myBean2 = new MyBean2();

        void disposeMyBean2(@Disposes MyBean2 myBean2) {
            MyBean2.actions.add("destroy");
        }

        @Produces
        @MyStereotype
        MyCloseableBean2 myCloseableBean2 = new MyCloseableBean2();

        void disposeMyCloseableBean2(@Disposes MyCloseableBean2 myBean2) {
            MyCloseableBean2.actions.add("destroy");
        }

        @Produces
        @MyStereotype
        MyBean3 myBean3() {
            return new MyBean3();
        }

        void disposeMyBean3(@Disposes MyBean3 myBean3) {
            MyBean3.actions.add("destroy");
        }

        @Produces
        @MyStereotype
        MyCloseableBean3 myCloseableBean3() {
            return new MyCloseableBean3();
        }

        void disposeMyCloseableBean3(@Disposes MyCloseableBean3 myCloseableBean3) {
            MyCloseableBean3.actions.add("destroy");
        }
    }

    static class MyBean2 {
        static final List<String> actions = new ArrayList<>();
    }

    static class MyCloseableBean2 implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @Override
        public void close() throws Exception {
            actions.add("close");
        }
    }

    static class MyBean3 {
        static final List<String> actions = new ArrayList<>();
    }

    static class MyCloseableBean3 implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @Override
        public void close() throws Exception {
            actions.add("close");
        }
    }

    static class MyBean4 {
        static final List<String> actions = new ArrayList<>();
    }

    static class MyCloseableBean4 implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @Override
        public void close() throws Exception {
            actions.add("close");
        }
    }

    static class MyBean4Creator implements BeanCreator<MyBean4> {
        @Override
        public MyBean4 create(SyntheticCreationalContext<MyBean4> context) {
            return new MyBean4();
        }
    }

    static class MyBean4Disposer implements BeanDestroyer<MyBean4> {
        @Override
        public void destroy(MyBean4 instance, SyntheticCreationalContext<MyBean4> context) {
            MyBean4.actions.add("destroy");
        }
    }

    static class MyCloseableBean4Creator implements BeanCreator<MyCloseableBean4> {
        @Override
        public MyCloseableBean4 create(SyntheticCreationalContext<MyCloseableBean4> context) {
            return new MyCloseableBean4();
        }
    }

    static class MyCloseableBean4Disposer implements BeanDestroyer<MyCloseableBean4> {
        @Override
        public void destroy(MyCloseableBean4 instance, SyntheticCreationalContext<MyCloseableBean4> context) {
            MyCloseableBean4.actions.add("destroy");
        }
    }
}
