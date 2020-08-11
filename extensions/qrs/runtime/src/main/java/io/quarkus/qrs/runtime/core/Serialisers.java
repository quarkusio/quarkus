package io.quarkus.qrs.runtime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;

public class Serialisers {

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private MultivaluedMap<Class<?>, ResourceWriter> writers = new MultivaluedHashMap<>();
    private MultivaluedMap<Class<?>, ResourceReader<?>> readers = new MultivaluedHashMap<>();

    public MessageBodyWriter<?> findWriter(Response response, QrsRequestContext requestContext) {
        Class<?> klass = response.getEntity().getClass();
        do {
            List<ResourceWriter> goodTypeWriters = writers.get(klass);
            if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
                List<MessageBodyWriter<?>> writers = new ArrayList<>(goodTypeWriters.size());
                for (ResourceWriter goodTypeWriter : goodTypeWriters) {
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

    public <T> void addWriter(Class<T> entityClass, ResourceWriter writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader<T> reader) {
        readers.add(entityClass, reader);
    }

    public List<ResourceWriter> findBuildTimeWriters(Class<?> entityType, String... produces) {
        List<MediaType> type = new ArrayList<>();
        for (String i : produces) {
            type.add(MediaType.valueOf(i));
        }
        return findBuildTimeWriters(entityType, type);
    }

    public List<ResourceWriter> findBuildTimeWriters(Class<?> entityType, List<MediaType> produces) {
        if (Response.class.isAssignableFrom(entityType)) {
            return Collections.emptyList();
        }
        //first we check to make sure that the return type is build time selectable
        //this fails when there are eligible writers for a sub type of the entity type
        //e.g. if the entity type is Object and there are mappers for String then we
        //can't determine the type at build time
        for (Map.Entry<Class<?>, List<ResourceWriter>> entry : writers.entrySet()) {
            if (entityType.isAssignableFrom(entry.getKey()) && !entry.getKey().equals(entityType)) {
                //this is a writer registered under a sub type
                //check to see if the media type is relevant
                if (produces == null || produces.isEmpty()) {
                    return null;
                } else {
                    for (ResourceWriter writer : entry.getValue()) {
                        MediaType match = MediaTypeHelper.getBestMatch(produces, writer.mediaTypes());
                        if (match != null) {
                            return null;
                        }
                    }
                }
            }

        }
        List<ResourceWriter> ret = new ArrayList<>();
        Class<?> klass = entityType;
        do {
            List<ResourceWriter> goodTypeWriters = writers.get(klass);
            if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
                for (ResourceWriter goodTypeWriter : goodTypeWriters) {
                    if (produces == null || produces.isEmpty()) {
                        ret.add(goodTypeWriter);
                    } else {
                        MediaType match = MediaTypeHelper.getBestMatch(produces, goodTypeWriter.mediaTypes());
                        if (match != null) {
                            ret.add(goodTypeWriter);
                        }
                    }
                }
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    public MultivaluedMap<Class<?>, ResourceWriter> getWriters() {
        return writers;
    }

    public MultivaluedMap<Class<?>, ResourceReader<?>> getReaders() {
        return readers;
    }

    public List<MessageBodyWriter<?>> findWriters(Class<?> entityType, MediaType resolvedMediaType) {
        List<MediaType> mt = Collections.singletonList(resolvedMediaType);
        List<MessageBodyWriter<?>> ret = new ArrayList<>();
        Class<?> klass = entityType;
        do {
            List<ResourceWriter> goodTypeWriters = writers.get(klass);
            if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
                for (ResourceWriter goodTypeWriter : goodTypeWriters) {
                    MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeWriter.mediaTypes());
                    if (match != null) {
                        ret.add(goodTypeWriter.getInstance());
                    }
                }
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }
}
