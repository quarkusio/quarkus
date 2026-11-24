package io.quarkus.arc.test.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Registration;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticInjections;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.supplement.SomeAsyncBeanInExternalLibrary;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

public class AsyncInvokerForBeanInExternalLibraryTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MyExtension.class, MyBeanCreator.class)
                    .addAsServiceProvider(BuildCompatibleExtension.class, MyExtension.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement", Version.getVersion())));

    @Inject
    MyBean bean;

    @Test
    public void test() throws Exception {
        assertEquals("Hello, world!", bean.doSomething().get());

        // the target bean of the generated invoker must be a non-application class,
        // otherwise this test wouldn't verify the classloading aspect
        assertTrue(SomeAsyncBeanInExternalLibrary.class.getClassLoader().getName()
                .contains("Quarkus Base Runtime ClassLoader"));
    }

    public static class MyBean {
        private final Invoker<SomeAsyncBeanInExternalLibrary, CompletableFuture<String>> invoker;

        MyBean(Invoker<SomeAsyncBeanInExternalLibrary, CompletableFuture<String>> invoker) {
            this.invoker = invoker;
        }

        public CompletableFuture<String> doSomething() throws Exception {
            return invoker.invoke(null, null);
        }
    }

    public static class MyExtension implements BuildCompatibleExtension {
        private InvokerInfo invoker;

        @Registration(types = SomeAsyncBeanInExternalLibrary.class)
        public void registration(BeanInfo bean, InvokerFactory invokers) {
            MethodInfo method = bean.declaringClass()
                    .methods()
                    .stream()
                    .filter(it -> it.name().equals("hello"))
                    .findAny()
                    .orElseThrow();
            invoker = invokers.createInvoker(bean, method)
                    .withInstanceLookup()
                    .build();
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            syn.addBean(MyBean.class)
                    .type(MyBean.class)
                    .scope(Dependent.class)
                    .withParam("invoker", invoker)
                    .createWith(MyBeanCreator.class);
        }
    }

    public static class MyBeanCreator implements SyntheticBeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticInjections injections, Parameters params) {
            return new MyBean(params.get("invoker", Invoker.class));
        }
    }
}
