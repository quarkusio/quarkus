package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface OtlpExporterTracesConfig extends OtlpExporterConfig {

    /**
     * If true, the Quarkus default OpenTelemetry exporter will always be instantiated, even when other exporters are present.
     * This will allow OTel data to be sent simultaneously to multiple destinations, including the default one. If false, the
     * Quarkus default OpenTelemetry exporter will not be instantiated if other exporters are present.
     * <p>
     * Defaults to <code>false</code>.
     */
    @WithName("experimental.dependent.default.enable")
    @WithDefault("false")
    boolean activateDefaultExporter();
}
