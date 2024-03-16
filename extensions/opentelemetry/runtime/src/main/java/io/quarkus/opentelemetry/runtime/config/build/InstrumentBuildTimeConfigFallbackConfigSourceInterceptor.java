package io.quarkus.opentelemetry.runtime.config.build;

import java.util.function.Function;

import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class InstrumentBuildTimeConfigFallbackConfigSourceInterceptor extends FallbackConfigSourceInterceptor {

    private static final Function<String, String> RENAME_FUNCTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (!s.startsWith("quarkus.otel.instrument.")) {
                return s;
            }

            if ("quarkus.otel.instrument.messaging".equals(s)) {
                return "quarkus.otel.instrument.reactive-messaging";
            }
            if ("quarkus.otel.instrument.resteasy-client".equals(s)) {
                return "quarkus.otel.instrument.rest-client-classic";
            }
            if ("quarkus.otel.instrument.rest".equals(s)) {
                return "quarkus.otel.instrument.resteasy-reactive";
            }
            if ("quarkus.otel.instrument.resteasy".equals(s)) {
                return "quarkus.otel.instrument.resteasy-classic";
            }

            return s;
        }
    };

    public InstrumentBuildTimeConfigFallbackConfigSourceInterceptor() {
        super(RENAME_FUNCTION);
    }
}
