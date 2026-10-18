package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;

public class EnricherFailureTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Cmd.class, FailingEnricher.class, MyReceiver.class));

    @Inject
    Signal<Cmd> signal;

    @Test
    public void testEnricherExceptionPropagatesOnRequest() {
        assertThatThrownBy(() -> signal.reactive().request(new Cmd("boom"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("enricher failed");
    }

    @Test
    public void testEnricherExceptionPropagatesOnPublish() {
        assertThatThrownBy(() -> signal.reactive().publish(new Cmd("boom"))
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("enricher failed");
    }

    @Test
    public void testEnricherExceptionPropagatesOnSend() {
        assertThatThrownBy(() -> signal.reactive().send(new Cmd("boom"))
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("enricher failed");
    }

    @Test
    public void testEnricherExceptionPropagatesOnBlockingRequest() {
        assertThatThrownBy(() -> signal.request(new Cmd("boom"), String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("enricher failed");
    }

    @Test
    public void testEnricherExceptionSwallowedOnFireAndForgetPublish() {
        // publish() is fire-and-forget; the exception is logged but not propagated
        signal.publish(new Cmd("boom"));
    }

    @Test
    public void testEnricherExceptionSwallowedOnFireAndForgetSend() {
        // send() is fire-and-forget; the exception is logged but not propagated
        signal.send(new Cmd("boom"));
    }

    record Cmd(String value) {
    }

    @Identifier("failing")
    @Singleton
    public static class FailingEnricher implements SignalMetadataEnricher {

        @Override
        public void enrich(EnrichmentContext context) {
            throw new IllegalStateException("enricher failed");
        }
    }

    @Singleton
    public static class MyReceiver {

        String onCmd(@Receives Cmd cmd) {
            return cmd.value();
        }
    }
}
