package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.AutoClose;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyCloseableBean.class);

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

    @AutoClose
    @Dependent
    static class MyBean {
        static final List<String> actions = new ArrayList<>();

        @PostConstruct
        void init() {
            actions.add("init");
        }

        @PreDestroy
        void destroy() {
            actions.add("destroy");
        }
    }

    @AutoClose
    @Dependent
    static class MyCloseableBean implements AutoCloseable {
        static final List<String> actions = new ArrayList<>();

        @PostConstruct
        void init() {
            actions.add("init");
        }

        @PreDestroy
        void destroy() {
            actions.add("destroy");
        }

        @Override
        public void close() {
            actions.add("close");
        }
    }
}
