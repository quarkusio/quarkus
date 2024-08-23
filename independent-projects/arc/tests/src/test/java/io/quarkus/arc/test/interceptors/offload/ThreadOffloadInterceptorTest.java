package io.quarkus.arc.test.interceptors.offload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;

public class ThreadOffloadInterceptorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, MyBean.class, FirstInterceptor.class,
            SecondInterceptor.class, ThirdInterceptor.class);

    @Test
    public void test() throws ExecutionException, InterruptedException {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertEquals("first: second: third: hello", bean.doSomething().get());
    }

    @Singleton
    static class MyBean {
        @Simple
        CompletableFuture<String> doSomething() {
            return CompletableFuture.completedFuture("hello");
        }
    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class FirstInterceptor {
        @AroundInvoke
        private Object aroundInvoke(InvocationContext ctx) {
            CompletableFuture<String> result = new CompletableFuture<>();
            try {
                ((CompletableFuture<String>) ctx.proceed()).whenComplete((value, error) -> {
                    if (error == null) {
                        result.complete("first: " + value);
                    } else {
                        result.completeExceptionally(error);
                    }
                });
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        }
    }

    @Simple
    @Priority(2)
    @Interceptor
    public static class SecondInterceptor {
        @AroundInvoke
        private Object aroundInvoke(InvocationContext ctx) {
            CompletableFuture<String> result = new CompletableFuture<>();
            new Thread(() -> {
                try {
                    ((CompletableFuture<String>) ctx.proceed()).whenComplete((value, error) -> {
                        if (error == null) {
                            result.complete("second: " + value);
                        } else {
                            result.completeExceptionally(error);
                        }
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }).start();
            return result;
        }
    }

    @Simple
    @Priority(3)
    @Interceptor
    public static class ThirdInterceptor {
        @AroundInvoke
        private Object aroundInvoke(InvocationContext ctx) {
            CompletableFuture<String> result = new CompletableFuture<>();
            try {
                ((CompletableFuture<String>) ctx.proceed()).whenComplete((value, error) -> {
                    if (error == null) {
                        result.complete("third: " + value);
                    } else {
                        result.completeExceptionally(error);
                    }
                });
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        }
    }
}
