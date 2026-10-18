package io.quarkus.jsonb;

import jakarta.json.bind.JsonbConfig;

/**
 * Meant to be implemented by a CDI bean that provided arbitrary customization for the default {@link JsonbConfig}.
 * <p>
 * All implementations (that are registered as CDI beans) are taken into account when producing the default {@link JsonbConfig}.
 * The {@link JsonbConfig} is in turn used to produce the default {@link jakarta.json.bind.Jsonb}
 * <p>
 * See also {@link JsonbProducer#jsonbConfig}.
 */
public interface JsonbConfigCustomizer {

    void customize(JsonbConfig jsonbConfig);
}
