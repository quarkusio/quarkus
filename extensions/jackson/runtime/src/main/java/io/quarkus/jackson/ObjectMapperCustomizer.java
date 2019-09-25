package io.quarkus.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the default {@link ObjectMapper}.
 * <p>
 * All implementations (that are registered as CDI beans) are taken into account when producing the default
 * {@link ObjectMapper}.
 * <p>
 * See also {@link ObjectMapperProducer#objectMapper}.
 */
public interface ObjectMapperCustomizer {

    void customize(ObjectMapper objectMapper);
}
