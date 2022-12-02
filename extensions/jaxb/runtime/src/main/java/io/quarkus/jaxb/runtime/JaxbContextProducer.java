package io.quarkus.jaxb.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class JaxbContextProducer {
    @DefaultBean
    @Singleton
    @Produces
    public JAXBContext jaxbContext(Instance<JaxbContextCustomizer> customizers) {
        try {
            Map<String, Object> properties = new HashMap<>();
            List<JaxbContextCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
            for (JaxbContextCustomizer customizer : sortedCustomizers) {
                customizer.customizeContextProperties(properties);
            }

            String[] classNamesToBeBounded = JaxbContextConfigRecorder.getClassesToBeBound();
            List<Class<?>> classes = new ArrayList<>();
            for (int i = 0; i < classNamesToBeBounded.length; i++) {
                Class<?> clazz = getClassByName(classNamesToBeBounded[i]);
                if (!clazz.isPrimitive()) {
                    classes.add(clazz);
                }
            }
            return JAXBContext.newInstance(classes.toArray(new Class[0]), properties);
        } catch (JAXBException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @DefaultBean
    @RequestScoped
    @Produces
    public Marshaller marshaller(JAXBContext jaxbContext, Instance<JaxbContextCustomizer> customizers) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            List<JaxbContextCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
            for (JaxbContextCustomizer customizer : sortedCustomizers) {
                customizer.customizeMarshaller(marshaller);
            }

            return marshaller;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @DefaultBean
    @RequestScoped
    @Produces
    public Unmarshaller unmarshaller(JAXBContext jaxbContext, Instance<JaxbContextCustomizer> customizers) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<JaxbContextCustomizer> sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers);
            for (JaxbContextCustomizer customizer : sortedCustomizers) {
                customizer.customizeUnmarshaller(unmarshaller);
            }

            return unmarshaller;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private List<JaxbContextCustomizer> sortCustomizersInDescendingPriorityOrder(Instance<JaxbContextCustomizer> customizers) {
        List<JaxbContextCustomizer> sortedCustomizers = new ArrayList<>();
        for (JaxbContextCustomizer customizer : customizers) {
            sortedCustomizers.add(customizer);
        }
        Collections.sort(sortedCustomizers);
        return sortedCustomizers;
    }

    private Class<?> getClassByName(String name) throws ClassNotFoundException {
        return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    }
}
