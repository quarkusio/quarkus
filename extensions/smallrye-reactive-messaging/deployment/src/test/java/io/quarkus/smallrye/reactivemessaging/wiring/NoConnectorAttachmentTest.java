package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoConnectorAttachmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MySource.class));

    @Inject
    MySource source;

    @Test
    public void testAutoAttachmentOfOutgoingChannel() {
        // TODO We could detect this at built time - emitter won't be injectable.
        assertThatThrownBy(() -> source.generate())
                .hasCauseInstanceOf(DefinitionException.class);
    }

    @ApplicationScoped
    static class MySource {

        @Channel("my-source")
        Emitter<Integer> emitter;

        public void generate() {
            for (int i = 0; i < 5; i++) {
                emitter.send(i);
            }
        }
    }

}
