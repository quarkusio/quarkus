package io.quarkus.qrs.runtime.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceResponseInterceptor;
import io.quarkus.qrs.runtime.model.ResourceWriter;

public class Serialisers {

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private MultivaluedMap<Class<?>, ResourceWriter<?>> writers = new MultivaluedHashMap<>();
    private MultivaluedMap<Class<?>, ResourceReader<?>> readers = new MultivaluedHashMap<>();

    public MessageBodyWriter<?> findWriter(Response response, RequestContext requestContext) {
        Class<?> klass = response.getEntity().getClass();
        do {
            List<ResourceWriter<?>> goodTypeWriters = writers.get(klass);
            if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
                List<MessageBodyWriter<?>> writers = new ArrayList<>(goodTypeWriters.size());
                for (ResourceWriter<?> goodTypeWriter : goodTypeWriters) {
                    writers.add(goodTypeWriter.getFactory().createInstance(requestContext).getInstance());
                }
                // FIXME: spec says to use content type sorting too
                for (MessageBodyWriter<?> writer : writers) {
                    // FIXME: those nulls
                    if (writer.isWriteable(response.getEntity().getClass(), null, null, response.getMediaType()))
                        return writer;
                }
                // not found any match, look up
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }

    public <T> void addWriter(Class<T> entityClass, ResourceWriter<T> writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader<T> reader) {
        readers.add(entityClass, reader);
    }

    public <T> ResourceWriter<T> findBuildTimeWriter(Class<T> entityType,
            List<ResourceResponseInterceptor> responseInterceptors) {
        if (entityType == Response.class)
            return null;
        for (ResourceResponseInterceptor responseInterceptor : responseInterceptors) {
            if (!responseInterceptor.isWriterSafe())
                return null;
        }
        Class<?> klass = entityType;
        do {
            List<ResourceWriter<?>> goodTypeWriters = writers.get(klass);
            if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
                // FIXME: spec says to use content type sorting too
                for (ResourceWriter<?> goodTypeWriter : goodTypeWriters) {
                    // FIXME: perhaps not optimise if we have more than one good writer?
                    if (goodTypeWriter.isBuildTimeSelectable())
                        return (ResourceWriter<T>) goodTypeWriter;
                }
                // no match, but we had entries, so let's not optimise
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }
}
