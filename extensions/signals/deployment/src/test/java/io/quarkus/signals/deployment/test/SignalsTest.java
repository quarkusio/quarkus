package io.quarkus.signals.deployment.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class SignalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MyReceivers.class, Foo.class, Reactive.class, Async.class));

    @Inject
    Signal<Foo> foo;

    @Inject
    @Reactive
    Signal<Foo> reactiveFoo;

    @Inject
    @Async
    Signal<Foo> asyncFoo;

    @Inject
    MyReceivers myReceivers;

    @Test
    public void testSignals() throws InterruptedException {
        myReceivers.sequence.clear();
        foo.publish(new Foo("pub_sub"));
        Awaitility.await().until(() -> myReceivers.sequence.size() >= 2);
        assertEquals(2, myReceivers.sequence.size());
        assertThat(myReceivers.sequence).contains("blocking_pub_sub", "blockingString_pub_sub");

        myReceivers.sequence.clear();
        foo.send(new Foo("one_to_one"));
        Awaitility.await().until(() -> myReceivers.sequence.size() >= 1);
        assertEquals(1, myReceivers.sequence.size());
        assertThat(myReceivers.sequence).containsAnyOf("blocking_one_to_one", "blockingString_one_to_one");

        myReceivers.sequence.clear();
        Uni<String> uni = reactiveFoo.reactive().request(new Foo("req"), String.class);
        assertEquals("REQ", uni.ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely());
        assertEquals(1, myReceivers.sequence.size());
        assertThat(myReceivers.sequence).contains("reactiveString_req");

        myReceivers.sequence.clear();
        assertEquals("req", foo.reactive().request(new Foo("REQ"), String.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely());
        assertEquals(1, myReceivers.sequence.size());
        assertThat(myReceivers.sequence).contains("blockingString_REQ");

        // CompletionStage receiver
        myReceivers.sequence.clear();
        assertEquals(3, asyncFoo.reactive().request(new Foo("req"), Integer.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely());
        assertEquals(1, myReceivers.sequence.size());
        assertThat(myReceivers.sequence).contains("completionStageInt_req");

        // No receiver matches the response type
        myReceivers.sequence.clear();
        assertNull(foo.reactive().request(new Foo("REQ"), int.class)
                .ifNoItem()
                .after(Duration.ofSeconds(1))
                .fail()
                .await().indefinitely());
        assertEquals(0, myReceivers.sequence.size());
    }

    // @Singleton added automatically
    public static class MyReceivers {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void blocking(MyService service, @Receives Foo foo) {
            if (!BlockingOperationControl.isBlockingAllowed()) {
                throw new IllegalStateException();
            }
            assertEquals(service.ping(), service.ping());
            sequence.add("blocking_" + foo.name());
        }

        String blockingString(@Receives Foo foo, BeanManager beanManager) {
            if (!BlockingOperationControl.isBlockingAllowed()) {
                throw new IllegalStateException();
            }
            if (beanManager == null) {
                throw new IllegalStateException("BeanManager is null");
            }
            sequence.add("blockingString_" + foo.name());
            return foo.name().toLowerCase();
        }

        Uni<String> reactiveString(@Receives @Reactive Foo foo) {
            if (BlockingOperationControl.isBlockingAllowed()) {
                return Uni.createFrom().failure(new IllegalStateException());
            }
            sequence.add("reactiveString_" + foo.name());
            return Uni.createFrom().item(foo.name().toUpperCase());
        }

        CompletionStage<Integer> completionStageInt(@Receives @Async Foo foo) {
            if (BlockingOperationControl.isBlockingAllowed()) {
                return CompletableFuture.failedFuture(new IllegalStateException());
            }
            sequence.add("completionStageInt_" + foo.name());
            return CompletableFuture.completedFuture(foo.name().length());
        }

    }

    record Foo(String name) {
    }

    @RequestScoped
    public static class MyService {

        private int val;

        public int ping() {
            return val;
        }

        @PostConstruct
        void init() {
            this.val = ThreadLocalRandom.current().nextInt();
        }

    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Reactive {

        final class Literal extends AnnotationLiteral<Reactive> implements Reactive {

            public static final Literal INSTANCE = new Literal();

            private static final long serialVersionUID = 1L;

        }

    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Async {

        final class Literal extends AnnotationLiteral<Async> implements Async {

            public static final Literal INSTANCE = new Literal();

            private static final long serialVersionUID = 1L;

        }

    }
}
