package io.quarkus.arc.test.clientproxy;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Reproduces the race in the lazily created client proxy of a normal-scoped bean: two threads that resolve
 * the same bean for the first time at the same moment both see {@code null} and each create their own proxy
 * instance. {@code QuarkusMock} then only reaches the proxy cached by the bean and misses the other one.
 */
public class ClientProxyConcurrentResolutionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Client.class);

    @Test
    public void concurrentResolutionReturnsSingleClientProxy() throws Exception {
        CountDownLatch bothInside = new CountDownLatch(2);
        CountDownLatch mayComplete = new CountDownLatch(1);
        Client.install(bothInside, mayComplete);

        AtomicReference<Client> first = new AtomicReference<>();
        AtomicReference<Client> second = new AtomicReference<>();

        Thread firstThread = new Thread(() -> first.set(Arc.container().instance(Client.class).get()));
        firstThread.start();
        Thread secondThread = new Thread(() -> second.set(Arc.container().instance(Client.class).get()));
        secondThread.start();

        // The client proxy extends Client, so its constructor runs inside the generated proxy() method, between
        // the null check and the field write. In the buggy implementation both threads reach this constructor and
        // release the latch; in the fixed implementation only one thread does and the other blocks on the monitor,
        // so the await below simply times out.
        bothInside.await(2, TimeUnit.SECONDS);
        mayComplete.countDown();

        firstThread.join();
        secondThread.join();

        assertSame(first.get(), second.get());
    }

    @ApplicationScoped
    static class Client {

        private static volatile CountDownLatch bothInside;
        private static volatile CountDownLatch mayComplete;

        static void install(CountDownLatch bothInside, CountDownLatch mayComplete) {
            Client.bothInside = bothInside;
            Client.mayComplete = mayComplete;
        }

        Client() {
            bothInside.countDown();
            try {
                mayComplete.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
