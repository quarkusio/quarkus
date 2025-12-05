package io.quarkus.vertx;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Vertx;

public class ScheduledTasksTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyBean.class));

    @Inject
    Vertx vertx;

    @Inject
    MyBean myBean;

    @Test
    void uniFromRegularThread() {
        String hello = myBean.delayedHello().await().atMost(Duration.ofSeconds(5));
        assertThat(hello, containsString("Hello!"));
        assertThat(hello, containsString("executor-thread-"));
    }

    @Test
    void uniFromEventLoop() {
        AtomicReference<String> result = new AtomicReference<>();
        vertx.runOnContext(v -> {
            myBean.delayedHello().subscribe().with(
                    result::set,
                    err -> result.set(err.getMessage()));
        });
        await().atMost(Duration.ofSeconds(5)).until(() -> result.get() != null);
        assertThat(result.get(), containsString("Hello!"));
        assertThat(result.get(), containsString("vert.x-eventloop-thread-"));
    }

    @Test
    void multiFromRegularThread() {
        LongAdder counter = new LongAdder();
        AtomicBoolean cancelled = new AtomicBoolean();
        ArrayList<String> items = new ArrayList<>();
        Cancellable cancellable = myBean.ticks()
                .onCancellation().invoke(() -> cancelled.set(true))
                .subscribe().with(tick -> {
                    items.add(tick);
                    counter.increment();
                });
        long start = System.currentTimeMillis();
        await().atMost(Duration.ofSeconds(5)).untilAdder(counter, greaterThan(3L));
        cancellable.cancel();
        await().atMost(Duration.ofSeconds(1)).untilTrue(cancelled);
        long duration = System.currentTimeMillis() - start;
        assertThat((long) items.size(), lessThanOrEqualTo(duration / 100L + 1L));
        assertThat(items.get(0), containsString("Hello executor-thread-"));
    }

    @Test
    void multiFromEventLoop() {
        LongAdder counter = new LongAdder();
        AtomicBoolean cancelled = new AtomicBoolean();
        ArrayList<String> items = new ArrayList<>();
        AtomicReference<Cancellable> cancellable = new AtomicReference<>();
        vertx.runOnContext(v -> {
            cancellable.set(myBean.ticks()
                    .onCancellation().invoke(() -> cancelled.set(true))
                    .subscribe().with(tick -> {
                        items.add(tick);
                        counter.increment();
                    }));
        });
        long start = System.currentTimeMillis();
        await().atMost(Duration.ofSeconds(5)).untilAdder(counter, greaterThan(3L));
        cancellable.get().cancel();
        await().atMost(Duration.ofSeconds(1)).untilTrue(cancelled);
        long duration = System.currentTimeMillis() - start;
        assertThat((long) items.size(), lessThanOrEqualTo(duration / 100L + 1L));
        assertThat(items.get(0), containsString("Hello vert.x-eventloop-thread-"));
    }

    @Test
    void retryFromRegularThread() {
        List<String> threadTraces = new ArrayList<>();
        UniAssertSubscriber<String> sub = UniAssertSubscriber.create();
        myBean.backoff(threadTraces).subscribe().withSubscriber(sub);
        sub.awaitFailure().assertFailedWith(IOException.class, "boom");
        assertThat(threadTraces, hasSize(6));
        assertThat(threadTraces, allOf((everyItem(not(containsString("vert.x-eventloop-thread-"))))));
    }

    @Test
    void retryFromEventLoop() {
        List<String> threadTraces = new ArrayList<>();
        UniAssertSubscriber<String> sub = UniAssertSubscriber.create();
        vertx.runOnContext(v -> myBean.backoff(threadTraces).subscribe().withSubscriber(sub));
        sub.awaitFailure().assertFailedWith(IOException.class, "boom");
        assertThat(threadTraces, hasSize(6));
        assertThat(threadTraces, allOf(everyItem(containsString("vert.x-eventloop-thread-"))));
    }

    @ApplicationScoped
    static class MyBean {

        public Uni<String> delayedHello() {
            return Uni.createFrom().item("Hello!")
                    .onItem().delayIt().by(Duration.ofSeconds(1L))
                    .onItem().transform(s -> s + " :: " + Thread.currentThread().getName());
        }

        public Multi<String> ticks() {
            return Multi.createFrom().ticks()
                    .every(Duration.ofMillis(100))
                    .onItem().transform(tick -> "Hello " + Thread.currentThread().getName());
        }

        public Uni<String> backoff(List<String> threadTraces) {
            return Uni.createFrom().<String> failure(new IOException("boom"))
                    .onSubscription().invoke(() -> threadTraces.add(Thread.currentThread().getName()))
                    .onFailure(IOException.class).retry().withBackOff(Duration.ofMillis(10)).atMost(5L);
        }
    }
}
