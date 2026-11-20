package io.quarkus.redis.lettuce.runtime.internal.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceBlockingRedisDataSourceImpl;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceReactiveRedisDataSourceImpl;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.TimeoutException;

/**
 * The blocking Pub/Sub group is the backend-agnostic {@code BlockingPubSubCommandsImpl} wrapping the Lettuce
 * reactive group; these tests check the wiring and that the blocking calls wait for Redis to confirm.
 */
class LettuceBlockingPubSubCommandsTest extends CommandsTestBase {

    final String channel = "channel-" + UUID.randomUUID();
    final Person luke = new Person("luke", "skywalker");
    final Person leia = new Person("leia", "skywalker");

    RedisDataSource blockingDs;
    PubSubCommands<Person> pubsub;

    @BeforeEach
    void initialize() {
        blockingDs = blockingDataSource();
        pubsub = blockingDs.pubsub(Person.class);
    }

    @AfterEach
    void noChannelLeftSubscribed() {
        Awaitility.await().untilAsserted(() -> {
            assertThat(connection.sync().pubsubChannels()).isEmpty();
            assertThat(connection.sync().pubsubNumpat()).isZero();
        });
    }

    @Test
    void getDataSource() {
        assertThat(pubsub.getDataSource()).isSameAs(blockingDs);
    }

    @Test
    void subscribeIsConfirmedBeforeItReturnsAndMessagesArriveOnADuplicatedContext() {
        long before = connectionCount();
        List<Person> people = new CopyOnWriteArrayList<>();
        List<Boolean> onDuplicatedContext = new CopyOnWriteArrayList<>();

        RedisSubscriber subscriber = pubsub.subscribe(channel, person -> {
            onDuplicatedContext.add(VertxContext.isOnDuplicatedContext());
            people.add(person);
        });
        // The blocking call returns only once Redis confirmed the subscription, so the channel is
        // visible and the dedicated connection is open right away.
        assertThat(connection.sync().pubsubChannels()).hasSize(1);
        assertThat(connectionCount()).isEqualTo(before + 1);

        pubsub.publish(channel, luke);
        pubsub.publish(channel, leia);
        Awaitility.await().until(() -> people.size() == 2);
        assertThat(people).containsExactly(luke, leia);
        assertThat(onDuplicatedContext).containsOnly(true);

        subscriber.unsubscribe();
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void channelListWithOnEndClosesAfterTheLastUnsubscribe() {
        String second = channel + "-second";
        List<String> receivedOn = new CopyOnWriteArrayList<>();
        AtomicInteger ended = new AtomicInteger();

        RedisSubscriber subscriber = pubsub.subscribe(List.of(channel, second), (name, person) -> receivedOn.add(name),
                ended::incrementAndGet, null);

        subscriber.unsubscribe(channel);
        assertThat(ended).hasValue(0);
        pubsub.publish(channel, luke);
        pubsub.publish(second, leia);
        Awaitility.await().until(() -> receivedOn.size() == 1);
        assertThat(receivedOn).containsExactly(second);

        subscriber.unsubscribe(second);
        Awaitility.await().until(() -> ended.get() == 1);
    }

    @Test
    void patternSubscriptionReportsTheChannelName() {
        List<String> channels = new CopyOnWriteArrayList<>();
        List<Person> people = new CopyOnWriteArrayList<>();

        RedisSubscriber subscriber = pubsub.subscribeToPattern(channel + "-*", (name, person) -> {
            channels.add(name);
            people.add(person);
        });
        assertThat(connection.sync().pubsubNumpat()).isEqualTo(1);

        pubsub.publish(channel, luke); // does not match
        pubsub.publish(channel + "-a", leia);
        Awaitility.await().until(() -> people.size() == 1);
        assertThat(people).containsExactly(leia);
        assertThat(channels).containsExactly(channel + "-a");

        subscriber.unsubscribe();
    }

    @Test
    void subscribeTimingOutDoesNotLeakTheConnection() {
        long before = connectionCount();
        // The connector hands out a connection only when the test says so, so the blocking call times out while
        // connecting; the connection opened afterwards must still be closed by the cancelled subscription.
        CompletableFuture<StatefulRedisPubSubConnection<byte[], byte[]>> gate = new CompletableFuture<>();
        RedisDataSource slow = new LettuceBlockingRedisDataSourceImpl(
                new LettuceReactiveRedisDataSourceImpl(vertx, lettuceConnection, pool(), () -> gate), Duration.ofMillis(200));

        assertThatThrownBy(() -> slow.pubsub(Person.class).subscribe(channel, person -> {
        })).isInstanceOf(TimeoutException.class);

        StatefulRedisPubSubConnection<byte[], byte[]> conn = redisClient.connectPubSub(codec, redisUri);
        gate.complete(conn);

        Awaitility.await().until(() -> !conn.isOpen());
        Awaitility.await().until(() -> connectionCount() == before);
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThatThrownBy(() -> pubsub.publish(null, luke)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe((String) null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(List.of(" "), p -> {
        })).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");
    }

}
