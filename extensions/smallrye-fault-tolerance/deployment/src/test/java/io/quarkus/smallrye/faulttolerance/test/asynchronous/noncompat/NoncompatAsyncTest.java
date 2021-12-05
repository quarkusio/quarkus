package io.quarkus.smallrye.faulttolerance.test.asynchronous.noncompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoncompatAsyncTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(NoncompatHelloService.class));

    @Inject
    NoncompatHelloService service;

    @Test
    public void noThreadOffloadAndFallback() throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        // no delay between retries, all executions happen on the same thread
        // if there _was_ a delay, subsequent retries would be offloaded to another thread
        assertThat(service.getHelloThreads()).allSatisfy(thread -> {
            assertThat(thread).isSameAs(mainThread);
        });
        assertThat(service.getHelloStackTraces()).allSatisfy(stackTrace -> {
            assertThat(stackTrace).anySatisfy(frame -> {
                assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
            });
        });

        // 1 initial execution + 3 retries
        assertThat(service.getInvocationCounter()).hasValue(4);

        assertThat(service.getFallbackThread()).isSameAs(mainThread);
    }
}
