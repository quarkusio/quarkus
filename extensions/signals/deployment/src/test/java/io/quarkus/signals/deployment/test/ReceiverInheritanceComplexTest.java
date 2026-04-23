package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ReceiverInheritanceComplexTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    Envelope.class,
                    IdService.class,
                    BaseReceiver.class,
                    InheritingReceiver.class,
                    OverridingReceiver.class));

    @Inject
    Signal<Envelope<String>> stringEnvelope;

    @Inject
    InheritingReceiver inheritingReceiver;

    @Inject
    OverridingReceiver overridingReceiver;

    @Test
    public void testInheritedReceiverWithInjectedParams() {
        inheritingReceiver.sequence.clear();

        Uni<Void> result = stringEnvelope.publishUni(new Envelope<>("alpha", "payload"));
        result.ifNoItem().after(Duration.ofSeconds(5)).fail().await().indefinitely();

        assertThat(inheritingReceiver.sequence).containsExactlyInAnyOrder("parent:alpha:true", "ctx:alpha");
    }

    @Test
    public void testInheritedRequestReplyWithSignalContext() {
        inheritingReceiver.sequence.clear();

        String reply = stringEnvelope
                .setMetadata(Map.of("priority", "high"))
                .requestUni(new Envelope<>("beta", "data"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals("beta:high", reply);
        assertThat(inheritingReceiver.sequence).containsExactly("ctx:beta");
    }

    @Test
    public void testOverriddenReceiverWithDifferentInjectedParams() {
        overridingReceiver.sequence.clear();

        Uni<Void> result = stringEnvelope.publishUni(new Envelope<>("gamma", "content"));
        result.ifNoItem().after(Duration.ofSeconds(5)).fail().await().indefinitely();

        assertThat(overridingReceiver.sequence).containsExactlyInAnyOrder("child:gamma:true", "ctx:gamma");
    }

    public record Envelope<T>(String id, T payload) {
    }

    @Singleton
    public static class IdService {
        public boolean isReady() {
            return true;
        }
    }

    public static abstract class BaseReceiver<T> {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void onEnvelope(BeanManager bm, @Receives Envelope<String> envelope, IdService idService) {
            if (bm == null) {
                throw new IllegalStateException("BeanManager is null");
            }
            sequence.add("parent:" + envelope.id() + ":" + idService.isReady());
        }

        Uni<String> onEnvelopeCtx(@Receives SignalContext<Envelope<String>> ctx) {
            sequence.add("ctx:" + ctx.signal().id());
            String priority = (String) ctx.metadata().get("priority");
            return Uni.createFrom().item(ctx.signal().id() + ":" + priority);
        }
    }

    @Singleton
    public static class InheritingReceiver extends BaseReceiver<String> {
        // Inherits both onEnvelope and onEnvelopeCtx
    }

    @Singleton
    public static class OverridingReceiver extends BaseReceiver<String> {
        @Override
        void onEnvelope(BeanManager bm, @Receives Envelope<String> envelope, IdService idService) {
            sequence.add("child:" + envelope.id() + ":" + idService.isReady());
        }
    }
}
