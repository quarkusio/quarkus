package io.quarkus.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.eventbus.Message;

public class CodecTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyBean.class, MyPetCodec.class));

    @Inject
    MyBean bean;

    @Inject
    Vertx vertx;

    @Test
    public void testWithGenericCodec() {
        Greeting hello = vertx.eventBus().<Greeting> request("person", new Person("bob", "morane"))
                .thenApply(Message::body)
                .toCompletableFuture().join();
        assertThat(hello.getMessage()).isEqualTo("Hello bob morane");
    }

    @Test
    public void testWithUserCodec() {
        Greeting hello = vertx.eventBus().<Greeting> request("pet", new Pet("neo", "rabbit"))
                .thenApply(Message::body)
                .toCompletableFuture().join();
        assertThat(hello.getMessage()).isEqualTo("Hello NEO");
    }

    static class Greeting {
        private final String message;

        Greeting(String message) {
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    static class MyBean {
        @ConsumeEvent("person")
        public CompletionStage<Greeting> hello(Person p) {
            return CompletableFuture.completedFuture(new Greeting("Hello " + p.getFirstName() + " " + p.getLastName()));
        }

        @ConsumeEvent(value = "pet", codec = MyPetCodec.class)
        public CompletionStage<Greeting> hello(Pet p) {
            return CompletableFuture.completedFuture(new Greeting("Hello " + p.getName()));
        }
    }

    // TODO Test with local set to false
}
