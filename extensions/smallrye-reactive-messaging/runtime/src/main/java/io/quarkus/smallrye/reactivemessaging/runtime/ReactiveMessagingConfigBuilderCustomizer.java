package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.function.Function;

import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class ReactiveMessagingConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {

    @Override
    public void configBuilder(SmallRyeConfigBuilder builder) {
        builder.withInterceptors(new FallbackConfigSourceInterceptor(new Fallback()));
        builder.withInterceptors(new RelocateConfigSourceInterceptor(new Relocate()));
    }

    private static final String REACTIVE_MESSAGING_PREFIX = "quarkus.messaging.";
    private static final String MP_MESSAGING_PREFIX = "mp.messaging.";
    private static final String INCOMING = "incoming.";
    private static final String OUTGOING = "outgoing.";

    private record Fallback() implements Function<String, String> {
        @Override
        public String apply(String name) {
            if (name.startsWith(REACTIVE_MESSAGING_PREFIX)
                    && (name.regionMatches(18, INCOMING, 0, 9) || name.regionMatches(18, OUTGOING, 0, 9))) {
                // replaces quarkus with mp
                return "mp" + name.substring(7);
            }
            return name;
        }
    }

    private record Relocate() implements Function<String, String> {
        @Override
        public String apply(String name) {
            if (name.startsWith(MP_MESSAGING_PREFIX)
                    && (name.regionMatches(13, INCOMING, 0, 9) || name.regionMatches(13, OUTGOING, 0, 9))) {
                // replaces mp with quarkus
                return "quarkus" + name.substring(2);
            }
            return name;
        }
    }
}
