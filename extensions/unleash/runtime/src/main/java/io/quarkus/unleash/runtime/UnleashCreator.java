package io.quarkus.unleash.runtime;

import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.Unleash;
import no.finn.unleash.util.UnleashConfig;

public class UnleashCreator {

    private final UnleashRuntimeTimeConfig unleashRuntimeTimeConfig;

    public UnleashCreator(UnleashRuntimeTimeConfig unleashRuntimeTimeConfig) {
        this.unleashRuntimeTimeConfig = unleashRuntimeTimeConfig;
    }

    public Unleash createUnleash() {
        UnleashConfig.Builder builder = UnleashConfig.builder()
                .unleashAPI(unleashRuntimeTimeConfig.api)
                .appName(unleashRuntimeTimeConfig.appName);

        unleashRuntimeTimeConfig.instanceId.ifPresent(builder::instanceId);

        if (unleashRuntimeTimeConfig.disableMetrics) {
            builder.disableMetrics();
        }

        return new DefaultUnleash(builder.build());
    }
}
