package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.AutoClose;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseDependentProducerFieldTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyConsumer.class, MyBeanProducer.class);

    @Test
    public void test() {
        Instance<MyConsumer> instance = Arc.container().select(MyConsumer.class);
        MyConsumer bean = instance.get();

        assertFalse(MyBean.closed);
        instance.destroy(bean);
        assertTrue(MyBean.closed);
    }

    @Dependent
    static class MyConsumer {
        @Inject
        MyBean bean;
    }

    @Singleton
    static class MyBeanProducer {
        @AutoClose
        @Dependent
        @Produces
        MyBean bean = new MyBean();
    }

    static class MyBean implements AutoCloseable {
        static boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }
}
