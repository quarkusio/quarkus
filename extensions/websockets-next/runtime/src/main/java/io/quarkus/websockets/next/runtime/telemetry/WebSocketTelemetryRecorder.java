package io.quarkus.websockets.next.runtime.telemetry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WebSocketTelemetryRecorder {

    public RuntimeValue<TelemetrySupportProvider> createEmptyTelemetrySupportProvider() {
        return new RuntimeValue<>(new TelemetrySupportProvider());
    }

    public Supplier<TelemetrySupportProvider> createTelemetrySupportProvider(
            List<Consumer<TelemetrySupportProviderBuilder>> builderCustomizers) {
        return new Supplier<>() {
            @Override
            public TelemetrySupportProvider get() {
                var builder = new TelemetrySupportProviderBuilder();
                for (Consumer<TelemetrySupportProviderBuilder> customizer : builderCustomizers) {
                    customizer.accept(builder);
                }
                return builder.build();
            }
        };
    }

}
