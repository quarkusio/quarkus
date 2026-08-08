package io.quarkus.arc.test.producer.disposer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithDependentWithoutDisposerTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyDependency.class, MyProducer.class);

    @Test
    public void test() {
        assertFalse(MyDependency.created);
        assertFalse(MyDependency.destroyed);

        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertEquals("Hello, world!", bean.hello());

        assertTrue(MyDependency.created);
        assertFalse(MyDependency.destroyed);

        Arc.container().beanManager().createInstance().destroy(bean);

        assertTrue(MyDependency.created);
        // this works even though the generated `_Bean` class doesn't have `destroy()`,
        // because it inherits one from `InjectableBean`
        assertTrue(MyDependency.destroyed);
    }

    static class MyBean {
        String hello() {
            return "Hello, world!";
        }
    }

    @Dependent
    static class MyDependency {
        static boolean created = false;
        static boolean destroyed = false;

        @PostConstruct
        void init() {
            created = true;
        }

        @PreDestroy
        void destroy() {
            destroyed = true;
        }
    }

    @ApplicationScoped
    static class MyProducer {
        @Produces
        @ApplicationScoped
        MyBean produce(MyDependency dependency) {
            return new MyBean();
        }
    }
}
