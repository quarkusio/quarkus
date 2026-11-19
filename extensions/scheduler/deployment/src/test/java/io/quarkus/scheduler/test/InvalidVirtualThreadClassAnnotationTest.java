package io.quarkus.scheduler.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

public class InvalidVirtualThreadClassAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .assertException(t -> assertThat(t).cause().isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("@Scheduled method cannot be non-blocking and annotated with @RunOnVirtualThread"))
            .withApplicationRoot(jar -> jar.addClass(InvalidBean.class));

    @Test
    void rejectsNonBlockingScheduledMethod() {
    }

    @ApplicationScoped
    @RunOnVirtualThread
    static class InvalidBean {

        @Scheduled(every = "1s")
        Uni<Void> tick() {
            return Uni.createFrom().voidItem();
        }
    }
}
