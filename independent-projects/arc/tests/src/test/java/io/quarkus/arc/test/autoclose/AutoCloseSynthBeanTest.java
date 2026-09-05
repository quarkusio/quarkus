package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseSynthBeanTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean.class)
                            .types(MyBean.class)
                            .scope(Dependent.class)
                            .autoClose(true)
                            .creator(MyBeanCreator.class)
                            .destroyer(MyBeanDisposer.class)
                            .done();

                    context.configure(MyCloseableBean.class)
                            .types(MyCloseableBean.class)
                            .scope(Dependent.class)
                            .autoClose(true)
                            .creator(MyCloseableBeanCreator.class)
                            .destroyer(MyCloseableBeanDisposer.class)
                            .done();
                }
            })
            .build();

    @Test
    public void testWithoutClose() {
        Instance<MyBean> instance = Arc.container().select(MyBean.class);
        assertEquals(List.of(), MyBean.actions);
        MyBean bean = instance.get();
        assertEquals(List.of("init"), MyBean.actions);
        instance.destroy(bean);
        assertEquals(List.of("init", "destroy"), MyBean.actions);
    }

    @Test
    public void testWithClose() {
        Instance<MyCloseableBean> instance = Arc.container().select(MyCloseableBean.class);
        assertEquals(List.of(), MyCloseableBean.actions);
        MyCloseableBean bean = instance.get();
        assertEquals(List.of("init"), MyCloseableBean.actions);
        instance.destroy(bean);
        assertEquals(List.of("init", "destroy", "close"), MyCloseableBean.actions);
    }

    static class MyBean {
        static final List<String> actions = new ArrayList<>();
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            MyBean.actions.add("init");
            return new MyBean();
        }
    }

    static class MyBeanDisposer implements BeanDestroyer<MyBean> {
        @Override
        public void destroy(MyBean instance, SyntheticCreationalContext<MyBean> context) {
            MyBean.actions.add("destroy");
        }
    }

    static class MyCloseableBean implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @Override
        public void close() {
            actions.add("close");
        }
    }

    static class MyCloseableBeanCreator implements BeanCreator<MyCloseableBean> {
        @Override
        public MyCloseableBean create(SyntheticCreationalContext<MyCloseableBean> context) {
            MyCloseableBean.actions.add("init");
            return new MyCloseableBean();
        }
    }

    static class MyCloseableBeanDisposer implements BeanDestroyer<MyCloseableBean> {
        @Override
        public void destroy(MyCloseableBean instance, SyntheticCreationalContext<MyCloseableBean> context) {
            MyCloseableBean.actions.add("destroy");
        }
    }
}
