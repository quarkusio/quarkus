package io.quarkus.arc.test.eager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

// this test relies on `ArcTestContainer` _not_ firing `Startup`!
public class EagerApplicationScopedSynthBeanTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean.class)
                            .types(MyBean.class)
                            .scope(ApplicationScoped.class)
                            .eager(true)
                            .creator(MyBeanCreator.class)
                            .done();
                }
            })
            .build();

    @Test
    public void test() {
        assertFalse(MyBean.constructed);

        Arc.container().beanManager().getEvent().fire(new Startup());

        assertTrue(MyBean.constructed);
    }

    static class MyBean {
        static boolean constructed = false;

        public MyBean() {
        }

        public MyBean(int unused) {
            constructed = true;
        }
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean(0);
        }
    }
}
