package io.quarkus.smallrye.faulttolerance.test.asynchronous.additional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NonblockingAsyncTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(NonblockingHelloService.class));

    @Inject
    NonblockingHelloService service;

    @Test
    public void noThreadOffloadAndFallback() throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(service.getHelloThread()).isSameAs(mainThread);
        assertThat(service.getHelloStackTrace()).anySatisfy(frame -> {
            assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
        });

        assertThat(service.getFallbackThread()).isSameAs(mainThread);
    }
}
