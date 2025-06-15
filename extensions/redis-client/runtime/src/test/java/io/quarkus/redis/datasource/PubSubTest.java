package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.runtime.datasource.ReactivePubSubCommandsImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

public class PubSubTest extends DatasourceTestBase {

    private ReactiveRedisDataSourceImpl ds;
    private static final Person person1 = new Person("luke", "skywalker");
    private static final Person person2 = new Person("anakin", "skywalker");
    private ReactivePubSubCommandsImpl<Person> ps;

    @BeforeEach
    void initialize() {
        ds = new ReactiveRedisDataSourceImpl(vertx, redis, api);
        ps = new ReactivePubSubCommandsImpl<>(ds, Person.class);
    }

    @AfterEach
    void tearDown() {
        ds.flushall().await().indefinitely();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(ps.getDataSource());
    }

    @Test
    void testWithSingleChannel() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        ReactivePubSubCommands.ReactiveRedisSubscriber subscriber = ps.subscribe("people", p -> latch.countDown())
                .await().indefinitely();

        ps.publish("people", person1).await().indefinitely();
        ps.publish("people", person2).await().indefinitely();

        ds.value(String.class, String.class).set("hello", "foo").await().indefinitely();

        ps.publish("people", person2).await().indefinitely();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        subscriber.unsubscribe().await().indefinitely();
    }

    @Test
    void testWithSinglePattern() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        ReactivePubSubCommands.ReactiveRedisSubscriber subscriber = ps.subscribeToPattern("peo*e", p -> {
            latch.countDown();
        }).await().indefinitely();

        ps.publish("people", person1).await().indefinitely();
        ps.publish("people", person2).await().indefinitely();

        ds.value(String.class, String.class).set("hello", "foo").await().indefinitely();

        ps.publish("people", person2).await().indefinitely();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        subscriber.unsubscribe().await().indefinitely();
    }

    @Test
    void testWithMultipleChannels() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        ReactivePubSubCommands.ReactiveRedisSubscriber subscriber = ps
                .subscribe(List.of("people1", "people2"), p -> latch.countDown()).await().indefinitely();

        ps.publish("people1", person1).await().indefinitely();
        ps.publish("people2", person2).await().indefinitely();

        ds.value(String.class, String.class).set("hello", "foo").await().indefinitely();

        ps.publish("people1", person2).await().indefinitely();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        subscriber.unsubscribe().await().indefinitely();
    }

    @Test
    void testWithMultipleChannelsAndASinglePattern() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(3);
        CountDownLatch latch2 = new CountDownLatch(2);
        ReactivePubSubCommands.ReactiveRedisSubscriber s1 = ps.subscribeToPattern("people*", p -> latch1.countDown())
                .await().indefinitely();
        ReactivePubSubCommands.ReactiveRedisSubscriber s2 = ps.subscribeToPattern("p*ple1", p -> latch2.countDown())
                .await().indefinitely();

        ps.publish("people1", person1).await().indefinitely();
        ps.publish("people2", person2).await().indefinitely();

        ds.value(String.class, String.class).set("hello", "foo").await().indefinitely();

        ps.publish("people1", person2).await().indefinitely();

        assertThat(latch1.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(latch2.await(5, TimeUnit.SECONDS)).isTrue();

        s1.unsubscribe().await().indefinitely();
        s2.unsubscribe().await().indefinitely();
    }

    @Test
    void testWithMulti() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1000);
        Multi<Person> multi = ps.subscribe("people");

        Cancellable cancellable = multi.subscribe().with(p -> latch.countDown());

        for (int i = 0; i < 1000; i++) {
            ps.publish("people", new Person("p" + i, "")).await().indefinitely();
        }

        ds.value(String.class, String.class).set("hello", "foo").await().indefinitely();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        cancellable.cancel();
    }

}
