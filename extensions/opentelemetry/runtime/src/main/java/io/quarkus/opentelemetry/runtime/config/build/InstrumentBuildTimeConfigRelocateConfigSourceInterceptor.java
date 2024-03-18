package io.quarkus.opentelemetry.runtime.config.build;

import java.util.function.Function;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * @deprecated maps the old config to the new config, should be removed at some point
 */
@Deprecated(forRemoval = true, since = "3.9")
public class InstrumentBuildTimeConfigRelocateConfigSourceInterceptor extends RelocateConfigSourceInterceptor {

    private static final Function<String, String> RENAME_FUNCTION = new Function<String, String>() {
        @Override
        public String apply(String s) {
            if (!s.startsWith("quarkus.otel.instrument.")) {
                return s;
            }

            if ("quarkus.otel.instrument.reactive-messaging".equals(s)) {
                return "quarkus.otel.instrument.messaging";
            }
            if ("quarkus.otel.instrument.rest-client-classic".equals(s)) {
                return "quarkus.otel.instrument.resteasy-client";
            }
            if ("quarkus.otel.instrument.resteasy-reactive".equals(s)) {
                return "quarkus.otel.instrument.rest";
            }
            if ("quarkus.otel.instrument.resteasy-classic".equals(s)) {
                return "quarkus.otel.instrument.resteasy";
            }

            return s;
        }
    };

    public InstrumentBuildTimeConfigRelocateConfigSourceInterceptor() {
        super(RENAME_FUNCTION);
    }
}
