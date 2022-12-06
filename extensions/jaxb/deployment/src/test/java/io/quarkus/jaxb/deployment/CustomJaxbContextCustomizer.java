package io.quarkus.jaxb.deployment;

import jakarta.inject.Singleton;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;

import io.quarkus.jaxb.runtime.JaxbContextCustomizer;

@Singleton
public class CustomJaxbContextCustomizer implements JaxbContextCustomizer {
    @Override
    public void customizeMarshaller(Marshaller marshaller) throws PropertyException {
        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
    }
}
