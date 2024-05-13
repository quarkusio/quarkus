package io.quarkus.resteasy.reactive.jaxb.runtime;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.JAXBContext;

import io.quarkus.jaxb.runtime.JaxbContextCustomizer;
import io.quarkus.jaxb.runtime.JaxbContextProducer;

public class JAXBContextContextResolver implements ContextResolver<JAXBContext> {

    private final ConcurrentHashMap<Class<?>, JAXBContext> cache = new ConcurrentHashMap<>();

    private final Instance<JaxbContextCustomizer> customizers;

    private final JaxbContextProducer jaxbContextProducer;

    public JAXBContextContextResolver(Instance<JaxbContextCustomizer> customizers,
            JaxbContextProducer jaxbContextProducer) {
        this.customizers = customizers;
        this.jaxbContextProducer = jaxbContextProducer;
    }

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
