package io.quarkus.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

public class EventBusCodecTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyBean.class, MyNonLocalBean.class,
                            MyPetCodec.class, Person.class, Pet.class,
                            Event.class, SubclassEvent.class));

    @Inject
    MyBean bean;

    /**
     * Bean setting the consumption to be non-local.
     * So, the user must configure the codec explicitly.
     */
    @Inject
    MyNonLocalBean nonLocalBean;

    @Inject
    Vertx vertx;

    @Test
    public void testWithGenericCodec() {
        Greeting hello = vertx.eventBus().<Greeting> request("person", new Person("bob", "morane"))
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(hello.getMessage()).isEqualTo("Hello bob morane");
    }

    @Test
    public void testWithUserCodec() {
        Greeting hello = vertx.eventBus().<Greeting> request("pet", new Pet("neo", "rabbit"))
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(hello.getMessage()).isEqualTo("Hello NEO");
    }

    @Test
    public void testWithUserCodecNonLocal() {
        Greeting hello = vertx.eventBus().<Greeting> request("nl-pet", new Pet("neo", "rabbit"))
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(hello.getMessage()).isEqualTo("Non Local Hello NEO");
    }

    @Test
    public void testWithSubclass() {
        Greeting hello = vertx.eventBus().<Greeting> request("subevent", new Event("my-event"))
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(hello.getMessage()).isEqualTo("Hello my-event");

        hello = vertx.eventBus().<Greeting> request("subevent", new SubclassEvent("my-subclass-event"))
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(hello.getMessage()).isEqualTo("Hello my-subclass-event");
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

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE_USE)
    @interface NonNull {
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

        // presence of this method is enough to verify that type annotation
        // on the message type doesn't cause failure
        @ConsumeEvent("message-type-with-type-annotation")
        void messageTypeWithTypeAnnotation(@NonNull Person person) {
        }

        // also register codec for subclasses
        @ConsumeEvent("subevent")
        public CompletionStage<Greeting> hello(Event event) {
            return CompletableFuture.completedFuture(new Greeting("Hello " + event.getProperty()));
        }
    }

    static class MyNonLocalBean {
        @ConsumeEvent(value = "nl-pet", codec = MyPetCodec.class, local = false)
        public CompletionStage<Greeting> hello(Pet p) {
            return CompletableFuture.completedFuture(new Greeting("Non Local Hello " + p.getName()));
        }
    }

}
