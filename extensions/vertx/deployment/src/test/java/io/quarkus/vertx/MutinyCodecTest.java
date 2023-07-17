package io.quarkus.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

public class MutinyCodecTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(MyBean.class, MyNonLocalBean.class,
                            MyPetCodec.class, Person.class, Pet.class));

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
        public Uni<Greeting> hello(Person p) {
            return Uni.createFrom().item(
                    () -> new Greeting("Hello " + p.getFirstName() + " " + p.getLastName()))
                    .emitOn(Infrastructure.getDefaultExecutor());
        }

        @ConsumeEvent(value = "pet", codec = MyPetCodec.class)
        public Uni<Greeting> hello(Pet p) {
            return Uni.createFrom().item(
                    () -> new Greeting("Hello " + p.getName()))
                    .emitOn(Infrastructure.getDefaultExecutor());
        }
    }

    static class MyNonLocalBean {
        @ConsumeEvent(value = "nl-pet", codec = MyPetCodec.class, local = false)
        public Uni<Greeting> hello(Pet p) {
            return Uni.createFrom().item(
                    () -> new Greeting("Non Local Hello " + p.getName()))
                    .emitOn(Infrastructure.getDefaultExecutor());
        }
    }

}
