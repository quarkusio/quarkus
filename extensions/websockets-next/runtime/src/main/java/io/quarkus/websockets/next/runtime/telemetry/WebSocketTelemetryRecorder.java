package io.quarkus.websockets.next.runtime.telemetry;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WebSocketTelemetryRecorder {

    public Function<SyntheticCreationalContext<WebSocketTelemetryProvider>, WebSocketTelemetryProvider> createTelemetryProvider() {
        return new Function<>() {
            @Override
            public WebSocketTelemetryProvider apply(SyntheticCreationalContext<WebSocketTelemetryProvider> ctx) {
                Instance<Consumer<WebSocketTelemetryProviderBuilder>> builderCustomizers = ctx
                        .getInjectedReference(new TypeLiteral<>() {
                        });
                var builder = new WebSocketTelemetryProviderBuilder();
                for (Consumer<WebSocketTelemetryProviderBuilder> customizer : builderCustomizers) {
                    customizer.accept(builder);
                }
                return builder.build();
            }
        };
    }

}
