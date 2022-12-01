package io.quarkus.jaxb.deployment;

import javax.inject.Singleton;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import io.quarkus.jaxb.runtime.JaxbContextCustomizer;

@Singleton
public class CustomJaxbContextCustomizer implements JaxbContextCustomizer {
    @Override
    public void customizeMarshaller(Marshaller marshaller) throws PropertyException {
        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
    }
}
