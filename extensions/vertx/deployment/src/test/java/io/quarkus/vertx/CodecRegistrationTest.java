package io.quarkus.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

public class CodecRegistrationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(EventBusConsumers.class));

    @Inject
    EventBusConsumers bean;

    @Inject
    Vertx vertx;

    @Test
    public void testReceptionOfString() {
        String address = "address-1";
        vertx.eventBus().send(address, "a");
        vertx.eventBus().send(address, "b");
        vertx.eventBus().send(address, "c");
        await().until(() -> bean.getAddress1().size() == 3);
    }

    @Test
    public void testReceptionOfStringAndSendingNothing() {
        String address = "address-2";
        vertx.eventBus().send(address, "a");
        vertx.eventBus().send(address, "b");
        vertx.eventBus().send(address, "c");
        await().until(() -> bean.getAddress2().size() == 3);

        List<Message<?>> messages = new CopyOnWriteArrayList<>();
        vertx.eventBus().request(address, "d").onItem().invoke(messages::add).subscribeAsCompletionStage();
        vertx.eventBus().request(address, "e").onItem().invoke(messages::add).subscribeAsCompletionStage();
        await().until(() -> messages.size() == 2);
        assertThat(messages.get(0).body()).isNull();
        assertThat(messages.get(1).body()).isNull();
    }

    @Test
    public void testWithPrimitiveTypes() {
        String address = "address-3";
        List<Message<Long>> messages = new CopyOnWriteArrayList<>();
        vertx.eventBus().<Long> request(address, 1).onItem().invoke(messages::add).subscribeAsCompletionStage();
        vertx.eventBus().<Long> request(address, 2).onItem().invoke(messages::add).subscribeAsCompletionStage();
        await().until(() -> messages.size() == 2);
        assertThat(messages.get(0).body()).isBetween(1L, 4L);
        assertThat(messages.get(1).body()).isBetween(1L, 4L);
    }

    @Test
    public void testWithPrimitiveTypesAndCompletionStage() {
        String address = "address-4";
        List<Message<Long>> messages = new CopyOnWriteArrayList<>();
        vertx.eventBus().<Long> request(address, 1).onItem().invoke(messages::add).subscribeAsCompletionStage();
        vertx.eventBus().<Long> request(address, 2).onItem().invoke(messages::add).subscribeAsCompletionStage();
        await().until(() -> messages.size() == 2);
        assertThat(messages.get(0).body()).isBetween(1L, 4L);
        assertThat(messages.get(1).body()).isBetween(1L, 4L);
    }

    @Test
    public void testCodecRegistrationBasedOnParameterType() {
        String address = "address-5";
        vertx.eventBus().send(address, new CustomType1("foo"));
        vertx.eventBus().send(address, new CustomType1("bar"));
        vertx.eventBus().send(address, new CustomType1("baz"));

        await().until(() -> bean.getSink().size() == 3);

        Set<String> set = bean.getSink().stream().map(x -> (CustomType1) x).map(CustomType1::getName)
                .collect(Collectors.toSet());
        assertThat(set).contains("foo", "bar", "baz");

        bean.getSink().clear();
        address = "address-6";
        vertx.eventBus().send(address, new CustomType1("foo-x"));
        vertx.eventBus().send(address, new CustomType1("bar-x"));
        vertx.eventBus().send(address, new CustomType1("baz-x"));

        await().until(() -> bean.getSink().size() == 3);
        set = bean.getSink().stream().map(x -> (CustomType1) x).map(CustomType1::getName)
                .collect(Collectors.toSet());
        assertThat(set).contains("foo-x", "bar-x", "baz-x");
    }

    @Test
    public void testCodecRegistrationBasedOnReturnType() {
        String address = "address-7";
        List<CustomType3> list = new CopyOnWriteArrayList<>();
        vertx.eventBus().<CustomType3> request(address, "foo").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();
        vertx.eventBus().<CustomType3> request(address, "bar").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();
        vertx.eventBus().<CustomType3> request(address, "baz").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();

        await().until(() -> list.size() == 3);

        Set<String> set = list.stream().map(CustomType3::getName).collect(Collectors.toSet());
        assertThat(set).contains("foo", "bar", "baz");

    }

    @Test
    public void testCodecRegistrationBasedOnReturnTypeWithCompletionStage() {
        String address = "address-8";
        List<CustomType4> list = new CopyOnWriteArrayList<>();
        vertx.eventBus().<CustomType4> request(address, "foo").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();
        vertx.eventBus().<CustomType4> request(address, "bar").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();
        vertx.eventBus().<CustomType4> request(address, "baz").onItem().invoke(m -> list.add(m.body()))
                .subscribeAsCompletionStage();

        await().until(() -> list.size() == 3);

        Set<String> set = list.stream().map(CustomType4::getName).collect(Collectors.toSet());
        assertThat(set).contains("foo", "bar", "baz");

    }

    @SuppressWarnings("unused")
    static class EventBusConsumers {

        private final List<String> address1 = new CopyOnWriteArrayList<>();
        private final List<String> address2 = new CopyOnWriteArrayList<>();

        public List<String> getAddress1() {
            return address1;
        }

        public List<String> getAddress2() {
            return address2;
        }

        @ConsumeEvent("address-1")
        void listenAddress1(String message) {
            address1.add(message);
        }

        @ConsumeEvent("address-2")
        CompletionStage<Void> listenAddress2(String message) {
            address2.add(message);
            return CompletableFuture.completedFuture(null);
        }

        @ConsumeEvent("address-3")
        long listenAddress3(int i) {
            return i + 1;
        }

        @ConsumeEvent("address-4")
        CompletionStage<Long> listenAddress4(int i) {
            return CompletableFuture.completedFuture((long) (i + 1));
        }

        List<Object> sink = new CopyOnWriteArrayList<>();

        @ConsumeEvent("address-5")
        void codecRegistrationBasedOnParam(CustomType1 ct) {
            sink.add(ct);
        }

        @ConsumeEvent("address-6")
        void codecRegistrationBasedOnParam(Message<CustomType2> ct) {
            sink.add(ct.body());
        }

        @ConsumeEvent("address-7")
        CustomType3 codecRegistrationBasedReturnType(String n) {
            return new CustomType3(n);
        }

        @ConsumeEvent("address-8")
        CompletionStage<CustomType4> codecRegistrationBasedReturnTypeAndCS(String n) {
            return CompletableFuture.completedFuture(new CustomType4(n));
        }

        public List<Object> getSink() {
            return sink;
        }

    }

    static class CustomType1 {
        private final String name;

        CustomType1(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "CustomType1{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    static class CustomType2 {
        private final String name;

        CustomType2(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class CustomType3 {
        private final String name;

        CustomType3(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class CustomType4 {
        private final String name;

        CustomType4(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
