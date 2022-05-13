package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigGroup
public class EventConfig {

    /**
     * Spans created for a mutiny stream can show a customizable text during subscription.
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<String> onSubscribe;
}
