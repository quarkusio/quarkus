package io.quarkus.smallrye.faulttolerance.test.asynchronous.context.propagation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FaultToleranceContextPropagationTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyAppService.class, MyReqService.class));

    @Inject
    MyAppService appService;

    @Inject
    MyReqService reqService;

    @Test
    @ActivateRequestContext
    public void test() throws ExecutionException, InterruptedException {
        String data = UUID.randomUUID().toString();

        reqService.setState(data);

        String result = appService.call().toCompletableFuture().get();
        assertThat(result).startsWith(data + "|");
        assertThat(result).isNotEqualTo(data + "|" + Thread.currentThread().getName());
    }

    @ApplicationScoped
    public static class MyAppService {
        @Inject
        MyReqService service;

        @Inject
        ManagedExecutor executor;

        @Asynchronous
        public CompletionStage<String> call() {
            return executor.supplyAsync(service::getStateAndThread);
        }
    }

    @RequestScoped
    public static class MyReqService {
        private String state;

        public void setState(String state) {
            this.state = state;
        }

        public String getStateAndThread() {
            return state + "|" + Thread.currentThread().getName();
        }
    }
}
