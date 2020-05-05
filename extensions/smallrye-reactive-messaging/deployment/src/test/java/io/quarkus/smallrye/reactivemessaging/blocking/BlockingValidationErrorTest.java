package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DefinitionException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Subscriber;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.annotations.Blocking;

public class BlockingValidationErrorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanReturningASubscriberOfMessages.class))
            .setExpectedException(DefinitionException.class);

    @Test
    public void runTest() {
        fail();
    }

    @ApplicationScoped
    public static class BeanReturningASubscriberOfMessages {
        private List<String> list = new ArrayList<>();

        @Blocking
        @Incoming("count")
        public Subscriber<Message<String>> create() {
            return ReactiveStreams.<Message<String>> builder().forEach(m -> list.add(m.getPayload()))
                    .build();
        }
    }
}
