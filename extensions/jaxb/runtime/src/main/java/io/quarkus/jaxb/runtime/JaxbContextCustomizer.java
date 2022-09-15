package io.quarkus.jaxb.runtime;

import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for the default {@link JAXBContext}.
 * <p>
 * All implementations (that are registered as CDI beans) are taken into account when producing the default
 * {@link JAXBContext}.
 * <p>
 * See also {@link JaxbContextProducer#jaxbContext}.
 */
public interface JaxbContextCustomizer extends Comparable<JaxbContextCustomizer> {

    int DEFAULT_PRIORITY = 0;

    default void customizeContextProperties(Map<String, Object> properties) {

    }

    default void customizeMarshaller(Marshaller marshaller) throws PropertyException {

    }

    default void customizeUnmarshaller(Unmarshaller unmarshaller) throws PropertyException {

    }

    /**
     * Defines the priority that the customizers are applied.
     * A lower integer value means that the customizer will be applied after a customizer with a higher priority
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    default int compareTo(JaxbContextCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
