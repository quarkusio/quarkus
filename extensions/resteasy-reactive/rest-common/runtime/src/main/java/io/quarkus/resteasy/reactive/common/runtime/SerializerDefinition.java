package io.quarkus.resteasy.reactive.common.runtime;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.RuntimeType;

import org.jboss.resteasy.reactive.common.core.SingletonBeanFactory;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.runtime.BeanContainer;

/**
 * Build time description of a {@code MessageBodyReader} or {@code MessageBodyWriter} to register with the
 * {@link org.jboss.resteasy.reactive.common.core.Serialisers} during static init.
 *
 * @param className the reader or writer class name
 * @param handledClassName the name of the entity class handled by the reader or writer
 * @param mediaTypeStrings the handled media types, empty to keep the defaults
 * @param constraint the runtime type the reader or writer is restricted to, or {@code null} for both
 * @param builtin whether the reader or writer is a built-in one
 * @param priority the priority of the reader or writer
 * @param singleton whether the class is a JAX-RS application singleton, otherwise it is looked up as an ArC bean
 */
public record SerializerDefinition(String className, String handledClassName, List<String> mediaTypeStrings,
        RuntimeType constraint, boolean builtin, Integer priority, boolean singleton) {

    public ResourceReader reader(BeanContainer beanContainer) {
        ResourceReader reader = new ResourceReader();
        reader.setBuiltin(builtin);
        reader.setFactory(factory(beanContainer));
        reader.setConstraint(constraint);
        if (!mediaTypeStrings.isEmpty()) {
            reader.setMediaTypeStrings(new ArrayList<>(mediaTypeStrings));
        }
        reader.setPriority(priority);
        return reader;
    }

    public ResourceWriter writer(BeanContainer beanContainer) {
        ResourceWriter writer = new ResourceWriter();
        writer.setBuiltin(builtin);
        writer.setFactory(factory(beanContainer));
        writer.setConstraint(constraint);
        if (!mediaTypeStrings.isEmpty()) {
            writer.setMediaTypeStrings(new ArrayList<>(mediaTypeStrings));
        }
        writer.setPriority(priority);
        return writer;
    }

    private <T> BeanFactory<T> factory(BeanContainer beanContainer) {
        if (singleton) {
            return new SingletonBeanFactory<>(className);
        }
        return new ArcBeanFactory<>(ResteasyReactiveCommonRecorder.loadClass(className), beanContainer);
    }
}
