package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.AutoClose;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseDependentBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyConsumer.class, MyBean.class, MyBean.class);

    @Test
    public void test() throws InterruptedException {
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

    @AutoClose
    @Dependent
    static class MyBean implements AutoCloseable {
        static boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }
}
