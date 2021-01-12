package io.quarkus.smallrye.faulttolerance.test.asynchronous.additional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BlockingAsyncTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(BlockingHelloService.class));

    @Inject
    BlockingHelloService service;

    @Test
    @Disabled // TODO: investigate why this is failing in CI, but not locally
    public void threadOffloadAndFallback() throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(service.getHelloThread()).isNotSameAs(mainThread);
        assertThat(service.getHelloStackTrace()).anySatisfy(frame -> {
            assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
        });

        assertThat(service.getFallbackThread()).isNotSameAs(mainThread);
    }
}
