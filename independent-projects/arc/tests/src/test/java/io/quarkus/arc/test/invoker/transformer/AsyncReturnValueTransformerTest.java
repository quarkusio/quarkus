package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;
import io.smallrye.mutiny.Uni;

public class AsyncReturnValueTransformerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo hello = bean.getImplClazz().firstMethod("hello");
                invokers.put(hello.name(), factory.createInvoker(bean, hello)
                        .withInstanceLookup()
                        .withReturnValueTransformer(Uni.class, "subscribeAsCompletionStage")
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, CompletionStage<String>> hello = helper.getInvoker("hello");
        assertEquals("hello", hello.invoke(null, null).toCompletableFuture().get());
    }

    @Singleton
    static class MyService {
        public Uni<String> hello() {
            return Uni.createFrom().item("hello");
        }
    }
}
