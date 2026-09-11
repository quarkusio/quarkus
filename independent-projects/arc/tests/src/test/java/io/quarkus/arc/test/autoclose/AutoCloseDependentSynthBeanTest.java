package io.quarkus.arc.test.autoclose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class AutoCloseDependentSynthBeanTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyConsumer.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean.class)
                            .types(MyBean.class)
                            .scope(Dependent.class)
                            .autoClose(true)
                            .creator(MyBeanCreator.class)
                            .done();
                }
            })
            .build();

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

    static class MyBean implements AutoCloseable {
        static boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }
}
