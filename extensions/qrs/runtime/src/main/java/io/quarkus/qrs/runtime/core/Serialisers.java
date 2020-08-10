package io.quarkus.qrs.runtime.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;

public class Serialisers {

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private MultivaluedMap<Class<?>, ResourceWriter<?>> writers = new MultivaluedHashMap<>();
    private MultivaluedMap<Class<?>, ResourceReader<?>> readers = new MultivaluedHashMap<>();

    public MessageBodyWriter<?> findWriter(Response response, QrsRequestContext requestContext) {
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
                    if (writer instanceof QrsMessageBodyWriter) {
                        if (((QrsMessageBodyWriter<?>) writer).isWriteable(response.getEntity().getClass(),
                                requestContext.getTarget().getLazyMethod(), response.getMediaType())) {
                            return writer;
                        }

                    } else {
                        if (writer.isWriteable(response.getEntity().getClass(),
                                requestContext.getTarget().getLazyMethod().getGenericReturnType(),
                                requestContext.getTarget().getLazyMethod().getAnnotations(), response.getMediaType())) {
                            return writer;
                        }
                    }
                }
                // not found any match, look up
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }

    public MessageBodyReader<?> findReader(Class<?> targetType, MediaType mediaType, QrsRequestContext requestContext) {
        Class<?> klass = targetType;
        do {
            List<ResourceReader<?>> goodTypeReaders = readers.get(klass);
            if (goodTypeReaders != null && !goodTypeReaders.isEmpty()) {
                List<MessageBodyReader<?>> readers = new ArrayList<>(goodTypeReaders.size());
                for (ResourceReader<?> goodTypeReader : goodTypeReaders) {
                    readers.add(goodTypeReader.getFactory().createInstance(requestContext).getInstance());
                }
                // FIXME: spec says to use content type sorting too
                for (MessageBodyReader<?> reader : readers) {
                    if (reader.isReadable(targetType, requestContext.getTarget().getLazyMethod().getGenericReturnType(),
                            requestContext.getTarget().getLazyMethod().getAnnotations(), mediaType))
                        return reader;
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

    public <T> ResourceWriter<T> findBuildTimeWriter(Class<T> entityType) {
        if (entityType == Response.class)
            return null;
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
