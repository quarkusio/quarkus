package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.pubsub.RedisPubSubMessage;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.mutiny.redis.client.Response;

public class PubSubCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;
    private PubSubCommands<Person> pubsub;
    private ReactivePubSubCommands<Person> reactive;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(5));
        pubsub = ds.pubsub(Person.class);

        var reactiveDS = new ReactiveRedisDataSourceImpl(vertx, redis, api);
        reactive = reactiveDS.pubsub(Person.class);
    }

    private final String channel = "channel-test";

    @AfterEach
    void clear() {
        awaitNoMoreActiveChannels();
        ds.flushall();
    }

    private void awaitNoMoreActiveChannels() {
        Awaitility.await().untilAsserted(() -> {
            Response response = api.pubsub(List.of("CHANNELS")).await().indefinitely();
            assertThat(response).isEmpty();
        });
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(pubsub.getDataSource());
    }

    @Test
    void testPubSub() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(channel, people::add);

        pubsub.publish(channel, new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people.size() == 1);

        pubsub.publish(channel, new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people.size() == 2);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void callbackCalledOnDuplicatedContext() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber sub1 = pubsub.subscribe(channel, p -> {
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
            people.add(p);
        });

        PubSubCommands.RedisSubscriber sub2 = pubsub.subscribeToPattern(channel, p -> {
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
            people.add(p);
        });

        pubsub.publish(channel, new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people.size() == 4);

        sub1.unsubscribe();
        sub2.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void testMultipleSubscribers() {

        List<Person> people1 = new CopyOnWriteArrayList<>();
        List<Person> people2 = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber1 = pubsub.subscribe(channel, people1::add);
        PubSubCommands.RedisSubscriber subscriber2 = pubsub.subscribeToPattern(channel + "*", p -> people2.add(p));

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish(channel + "-another", new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 1);
        Awaitility.await().until(() -> people2.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel + "-yet", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 3);
        Awaitility.await().until(() -> people2.size() == 5);

        subscriber2.unsubscribe();
        subscriber1.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void testMultipleSubscribersAndBiConsumer() {

        List<Person> people1 = new CopyOnWriteArrayList<>();
        List<Person> people2 = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber1 = pubsub.subscribe(channel, people1::add);
        PubSubCommands.RedisSubscriber subscriber2 = pubsub.subscribeToPattern(channel + "*", (s, p) -> {
            assertThat(s).isIn(channel, channel + "-another", channel + "-yet");
            people2.add(p);
        });

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish(channel + "-another", new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 1);
        Awaitility.await().until(() -> people2.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel + "-yet", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 3);
        Awaitility.await().until(() -> people2.size() == 5);

        subscriber2.unsubscribe();
        subscriber1.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void invalidSubscribe() {
        assertThatThrownBy(() -> pubsub.subscribe("", p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe((String) null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe((List) null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe(Collections.emptyList(), p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribe("aaa", null)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> pubsub.subscribeToPattern("", p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPattern(null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(null, p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPatterns(Collections.emptyList(), p -> {
        })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPattern("aaa", (Consumer<Person>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pubsub.subscribeToPattern("aaa", (BiConsumer<String, Person>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subscribeToMultipleChannels() {

        List<Person> people1 = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(List.of(channel, channel + "1"), people1::add);

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish(channel + "1", new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel + "1", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 5);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToMultiplePatterns() {
        List<Person> people1 = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribeToPatterns(List.of(channel + "*", "foo"),
                p -> people1.add(p));

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish("foo", new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish("foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 5);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToMultiplePatternsWithBiConsumer() {
        List<Person> people1 = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribeToPatterns(List.of(channel + "*", "foo"),
                (s, p) -> {
                    assertThat(s).isIn("foo", channel);
                    people1.add(p);
                });

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish("foo", new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 2);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish("foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 5);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToMultiplePatternsWithMulti() {
        List<Person> people1 = new CopyOnWriteArrayList<>();
        Multi<Person> multi = reactive.subscribeToPatterns(channel + "*", "foo");

        Cancellable cancellable = multi.subscribe().with(people1::add);

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish("foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() > 1);

        cancellable.cancel();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToMultiplePatternsWithMultiAsMessages() {
        List<RedisPubSubMessage<Person>> people = new CopyOnWriteArrayList<>();
        Multi<RedisPubSubMessage<Person>> multi = reactive.subscribeAsMessagesToPatterns(channel + "*", "foo");

        Cancellable cancellable = multi.subscribe().with(people::add);

        pubsub.publish(channel, new Person("luke", "skywalker"));
        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish("foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people.size() > 1);

        assertThat(people).allSatisfy(m -> {
            assertThat(m.getChannel()).isNotBlank();
            assertThat(m.getPayload()).isNotNull();
        });

        cancellable.cancel();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToSingleWithMulti() {
        List<Person> people1 = new CopyOnWriteArrayList<>();
        Multi<Person> multi = reactive.subscribeToPatterns(channel + "*");

        Cancellable cancellable = multi.subscribe().with(people1::add);

        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish(channel, new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 1);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel + "foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people1.size() == 4);

        cancellable.cancel();

        awaitNoMoreActiveChannels();

    }

    @Test
    void subscribeToSingleWithMultiAsMessages() {
        List<RedisPubSubMessage<Person>> people = new CopyOnWriteArrayList<>();
        Multi<RedisPubSubMessage<Person>> multi = reactive.subscribeAsMessagesToPatterns(channel + "*");

        Cancellable cancellable = multi.subscribe().with(people::add);

        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish(channel, new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people.size() == 1);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel + "foo", new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people.size() == 4);

        assertThat(people).allSatisfy(m -> {
            assertThat(m.getChannel()).isNotBlank();
            assertThat(m.getPayload()).isNotNull();
        });

        cancellable.cancel();

        awaitNoMoreActiveChannels();

    }

    @Test
    void testSubscribeAsMessages() {
        List<RedisPubSubMessage<Person>> people = new CopyOnWriteArrayList<>();
        Multi<RedisPubSubMessage<Person>> multi = reactive.subscribeAsMessages(channel);

        Cancellable cancellable = multi.subscribe().with(people::add);

        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish(channel, new Person("luke", "skywalker"));

        Awaitility.await().until(() -> people.size() == 1);

        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));
        pubsub.publish(channel, new Person("leia", "skywalker"));

        Awaitility.await().until(() -> people.size() == 4);

        assertThat(people).allSatisfy(m -> {
            assertThat(m.getChannel()).isNotBlank();
            assertThat(m.getPayload()).isNotNull();
        });

        cancellable.cancel();

        awaitNoMoreActiveChannels();

    }

    @Test
    void unsubscribe() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(channel, people::add);
        pubsub.publish(channel, new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 1);

        subscriber.unsubscribe(channel);

        awaitNoMoreActiveChannels();

        pubsub.publish(channel, new Person("leia", "skywalker"));

        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> people.size() == 1);

    }

    @Test
    void unsubscribeAll() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(channel, people::add);
        pubsub.publish(channel, new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 1);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

        pubsub.publish(channel, new Person("leia", "skywalker"));

        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> people.size() == 1);
    }

    @Test
    void unsubscribeOne() {
        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(List.of("foo", "bar"), people::add);
        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish("bar", new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 2);

        subscriber.unsubscribe("foo");

        pubsub.publish("foo", new Person("luke", "skywalker"));
        pubsub.publish("bar", new Person("luke", "skywalker"));

        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> people.size() > 2);

        subscriber.unsubscribe("bar");

        awaitNoMoreActiveChannels();
    }

    @Test
    void unsubscribePattern() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribeToPattern(channel + "*", p -> people.add(p));
        pubsub.publish(channel + "1", new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 1);

        subscriber.unsubscribe(channel + "*");

        awaitNoMoreActiveChannels();
        pubsub.publish(channel + "1", new Person("leia", "skywalker"));

        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> people.size() == 1);
    }

    @Test
    void unsubscribeAllPatterns() {

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribeToPattern(channel + "*", p -> people.add(p));
        pubsub.publish(channel + "1", new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 1);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

        pubsub.publish(channel + "1", new Person("leia", "skywalker"));

        Awaitility.await().pollDelay(Duration.ofMillis(10)).until(() -> people.size() == 1);

    }

    @Test
    void unsubscribeOnePattern() {
        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribeToPatterns(List.of("foo*", "bar*"), p -> people.add(p));
        pubsub.publish("foo1", new Person("luke", "skywalker"));
        pubsub.publish("bar2", new Person("luke", "skywalker"));
        Awaitility.await().until(() -> people.size() == 2);

        subscriber.unsubscribe("foo*");

        pubsub.publish("foo3", new Person("luke", "skywalker"));
        pubsub.publish("bar4", new Person("luke", "skywalker"));

        subscriber.unsubscribe("bar*");

        awaitNoMoreActiveChannels();
    }

    @Test
    void utf8ChannelAndContent() {
        String channel = "channelλ";
        String name = "αβγ";

        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(channel, people::add);

        pubsub.publish(channel, new Person(name, name));

        Awaitility.await().until(() -> people.size() == 1);
        Assertions.assertThat(people).containsExactly(new Person(name, name));

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();
    }

    @Test
    void testPubSubWithTypeReference() {
        var pubsub = ds.pubsub(new TypeReference<Map<String, Person>>() {
            // Empty on purpose
        });
        List<Person> people = new CopyOnWriteArrayList<>();
        PubSubCommands.RedisSubscriber subscriber = pubsub.subscribe(channel, map -> people.addAll(map.values()));

        pubsub.publish(channel, Map.of("luke", new Person("luke", "skywalker")));

        Awaitility.await().until(() -> people.size() == 1);

        pubsub.publish(channel, Map.of("leia", new Person("leia", "skywalker")));

        Awaitility.await().until(() -> people.size() == 2);

        subscriber.unsubscribe();

        awaitNoMoreActiveChannels();

    }

}
