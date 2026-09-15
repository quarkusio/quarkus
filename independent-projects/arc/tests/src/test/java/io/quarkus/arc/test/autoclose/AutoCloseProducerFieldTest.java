package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.AutoClose;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseProducerFieldTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBeanProducer.class, MyCloseableBeanProducer.class);

    @Test
    public void testWithoutClose() {
        Instance<MyBean> instance = Arc.container().select(MyBean.class);
        assertEquals(List.of(), MyBeanProducer.actions);
        MyBean bean = instance.get();
        assertEquals(List.of("init"), MyBeanProducer.actions);
        instance.destroy(bean);
        assertEquals(List.of("init", "destroy"), MyBeanProducer.actions);
    }

    @Test
    public void testWithClose() {
        Instance<MyCloseableBean> instance = Arc.container().select(MyCloseableBean.class);
        assertEquals(List.of(), MyCloseableBeanProducer.actions);
        MyCloseableBean bean = instance.get();
        assertEquals(List.of("init"), MyCloseableBeanProducer.actions);
        instance.destroy(bean);
        assertEquals(List.of("init", "destroy", "close"), MyCloseableBeanProducer.actions);
    }

    @Singleton
    static class MyBeanProducer {
        static final List<String> actions = new ArrayList<>();

        @AutoClose
        @Dependent
        @Produces
        MyBean bean = new MyBean();

        @PostConstruct
        void init() {
            actions.add("init");
        }

        void dispose(@Disposes MyBean bean) {
            actions.add("destroy");
        }
    }

    static class MyBean {
    }

    @Singleton
    static class MyCloseableBeanProducer {
        static final List<String> actions = new ArrayList<>();

        @AutoClose
        @Dependent
        @Produces
        MyCloseableBean bean = new MyCloseableBean();

        @PostConstruct
        void init() {
            actions.add("init");
        }

        void dispose(@Disposes MyCloseableBean bean) {
            actions.add("destroy");
        }
    }

    static class MyCloseableBean implements AutoCloseable {
        @Override
        public void close() {
            MyCloseableBeanProducer.actions.add("close");
        }
    }
}
