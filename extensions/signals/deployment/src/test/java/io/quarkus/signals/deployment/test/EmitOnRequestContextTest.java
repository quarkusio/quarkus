package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that the CDI request context activated by the built-in {@code RequestContextInterceptor}
 * is terminated correctly even when a reactive receiver uses {@code emitOn} to switch
 * the executor for downstream Uni operators.
 */
public class EmitOnRequestContextTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MyReceiver.class, RequestScopedService.class, Cmd.class));

    @Inject
    Signal<Cmd> signal;

    @Inject
    MyReceiver receiver;

    @Test
    public void testRequestContextTerminatedAfterEmitOn() {
        RequestScopedService.DESTROYED.clear();
        receiver.threadNames.clear();

        String result = signal.reactive().request(new Cmd(), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertThat(result).isEqualTo("done");
        // emitOn must have switched threads
        assertThat(receiver.threadNames).hasSize(2);
        assertThat(receiver.threadNames.get(0)).isNotEqualTo(receiver.threadNames.get(1));
        // the request context must have been terminated
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(RequestScopedService.DESTROYED).hasSize(1));
        assertThat(RequestScopedService.DESTROYED.get(0)).isEqualTo(receiver.threadNames.get(0));
    }

    @Singleton
    public static class MyReceiver {

        final List<String> threadNames = new CopyOnWriteArrayList<>();

        private final ExecutorService customExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("custom-emit-on-thread");
            t.setDaemon(true);
            return t;
        });

        Uni<String> onCmd(@Receives Cmd cmd, RequestScopedService service) {
            threadNames.add(Thread.currentThread().getName());
            service.touch();
            return Uni.createFrom().item("done")
                    .emitOn(customExecutor)
                    .onItem().invoke(s -> threadNames.add(Thread.currentThread().getName()));
        }
    }

    @RequestScoped
    public static class RequestScopedService {

        static final List<String> DESTROYED = new CopyOnWriteArrayList<>();

        void touch() {
        }

        @PreDestroy
        void destroy() {
            DESTROYED.add(Thread.currentThread().getName());
        }
    }

    record Cmd() {
    }
}
