package io.quarkus.arc.test.buildextension.beans;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.Dependent;

import org.jboss.jandex.ClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * A synthetic bean may be created lazily from a thread whose context class loader is not the application class
 * loader, e.g. a thread created by a third-party library.
 */
public class SyntheticInjectionPointForeignThreadTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependency.class)
            .beanRegistrars(new TestRegistrar())
            .build();

    @Test
    public void createFromThreadWithForeignContextClassLoader() throws Exception {
        CompletableFuture<MyBean> result = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try {
                result.complete(Arc.container().instance(MyBean.class).get());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        try (URLClassLoader foreignClassLoader = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(foreignClassLoader);
            thread.start();

            assertThat(result.get(10, TimeUnit.SECONDS).dep).isNotNull();
        }
    }

    @Dependent
    static class MyDependency {
    }

    static class MyBean {
        final MyDependency dep;

        MyBean(MyDependency dep) {
            this.dep = dep;
        }
    }

    static class TestRegistrar implements BeanRegistrar {
        @Override
        public void register(RegistrationContext context) {
            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .scope(BuiltinScope.DEPENDENT.getInfo())
                    .addInjectionPoint(ClassType.create(MyDependency.class))
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean(context.getInjectedReference(MyDependency.class));
        }
    }
}
