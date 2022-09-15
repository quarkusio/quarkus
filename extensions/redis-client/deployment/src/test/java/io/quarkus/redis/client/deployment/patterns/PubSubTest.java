package io.quarkus.redis.client.deployment.patterns;

import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.deployment.RedisTestResource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class PubSubTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyCache.class)
                    .addClass(BusinessObject.class).addClass(Notification.class).addClass(MySubscriber.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    MyCache cache;

    @Inject
    MySubscriber subscriber;

    @Test
    void cacheWithPubSub() {
        BusinessObject foo = cache.get("ps-foo");
        BusinessObject bar = cache.get("ps-bar");
        Assertions.assertNull(foo);
        Assertions.assertNull(bar);

        cache.set("ps-foo", new BusinessObject("ps-foo"));
        cache.set("ps-bar", new BusinessObject("ps-bar"));

        await().until(() -> subscriber.list().size() == 2);
    }

    public static final class BusinessObject {
        public String result;

        public BusinessObject() {

        }

        public BusinessObject(String v) {
            this.result = v;
        }
    }

    public static final class Notification {
        public String key;
        public BusinessObject bo;

        public Notification() {

        }

        public Notification(String key, BusinessObject bo) {
            this.key = key;
            this.bo = bo;
        }
    }

    @ApplicationScoped
    @Startup
    public static class MySubscriber implements Consumer<Notification> {
        private final PubSubCommands<Notification> pub;
        private final PubSubCommands.RedisSubscriber subscriber;

        public List<Notification> list = new ArrayList<>();

        public MySubscriber(RedisDataSource ds) {
            pub = ds.pubsub(Notification.class);
            subscriber = pub.subscribe("notifications", this);
        }

        @PreDestroy
        public void terminate() {
            subscriber.unsubscribe();
        }

        @Override
        public void accept(Notification notification) {
            // Received the notification
            list.add(notification);
        }

        public List<Notification> list() {
            return list;
        }
    }

    @ApplicationScoped
    public static class MyCache {

        private final ValueCommands<String, BusinessObject> commands;
        private final PubSubCommands<Notification> pub;

        public MyCache(RedisDataSource ds) {
            commands = ds.value(BusinessObject.class);
            pub = ds.pubsub(Notification.class);
        }

        public BusinessObject get(String key) {
            return commands.get(key);
        }

        public void set(String key, BusinessObject bo) {
            commands.set(key, bo);
            pub.publish("notifications", new Notification(key, bo));
        }

    }

}
