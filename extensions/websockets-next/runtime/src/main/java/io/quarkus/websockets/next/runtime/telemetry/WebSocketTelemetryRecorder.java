package io.quarkus.websockets.next.runtime.telemetry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WebSocketTelemetryRecorder {

    public Supplier<WebsocketTelemetryProvider> createTelemetryProvider(
            List<Consumer<WebsocketTelemetryProviderBuilder>> builderCustomizers) {
        return new Supplier<>() {
            @Override
            public WebsocketTelemetryProvider get() {
                var builder = new WebsocketTelemetryProviderBuilder();
                for (Consumer<WebsocketTelemetryProviderBuilder> customizer : builderCustomizers) {
                    customizer.accept(builder);
                }
                return builder.build();
            }
        };
    }

}
