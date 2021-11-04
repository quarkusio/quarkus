package io.quarkus.smallrye.faulttolerance.test.asynchronous.additional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BlockingAsyncTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BlockingHelloService.class));

    @Inject
    BlockingHelloService service;

    @Test
    public void threadOffloadAndFallback() throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(service.getHelloThreads()).allSatisfy(thread -> {
            assertThat(thread).isNotSameAs(mainThread);
        });
        assertThat(service.getHelloStackTraces()).allSatisfy(stackTrace -> {
            assertThat(stackTrace).anySatisfy(frame -> {
                assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
            });
        });

        // 1 initial execution + 3 retries
        assertThat(service.getInvocationCounter()).hasValue(4);

        assertThat(service.getFallbackThread()).isNotSameAs(mainThread);
    }
}
