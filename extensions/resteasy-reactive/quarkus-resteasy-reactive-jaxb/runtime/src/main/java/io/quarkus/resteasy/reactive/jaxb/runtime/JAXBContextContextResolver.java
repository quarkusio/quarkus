package io.quarkus.resteasy.reactive.jaxb.runtime;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.JAXBContext;

import io.quarkus.jaxb.runtime.JaxbContextCustomizer;
import io.quarkus.jaxb.runtime.JaxbContextProducer;

public class JAXBContextContextResolver implements ContextResolver<JAXBContext> {

    private ConcurrentHashMap<Class<?>, JAXBContext> cache = new ConcurrentHashMap<>();

    @Inject
    Instance<JaxbContextCustomizer> customizers;

    @Inject
    JaxbContextProducer jaxbContextProducer;

    @Override
    public JAXBContext getContext(Class<?> clazz) {
        JAXBContext jaxbContext = cache.get(clazz);
        if (jaxbContext == null) {
            jaxbContext = jaxbContextProducer.createJAXBContext(customizers, clazz);
            cache.put(clazz, jaxbContext);
        }

        return jaxbContext;
    }
}
