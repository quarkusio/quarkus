package io.quarkus.qrs.runtime.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.qrs.runtime.model.ResourceReader;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;
import io.vertx.core.buffer.Buffer;

public class Serialisers {

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private MultivaluedMap<Class<?>, ResourceWriter> writers = new MultivaluedHashMap<>();
    private MultivaluedMap<Class<?>, ResourceReader<?>> readers = new MultivaluedHashMap<>();

    public static final List<MediaType> WILDCARD_LIST = Collections.singletonList(MediaType.WILDCARD_TYPE);
    public static final MessageBodyWriter[] EMPTY = new MessageBodyWriter[0];
    private final ConcurrentMap<Class<?>, Holder> noMediaTypeClassCache = new ConcurrentHashMap<>();
    private Function<Class<?>, Holder> mappingFunction = new Function<Class<?>, Holder>() {
        @Override
        public Holder apply(Class<?> aClass) {
            Class<?> c = aClass;
            Set<MediaType> types = new LinkedHashSet<>();
            List<ResourceWriter> writers = new ArrayList<>();
            Set<Class<?>> seenInterfaces = new HashSet<>();
            while (c != null) {
                List<ResourceWriter> forClass = getWriters().get(c);
                if (forClass != null) {
                    for (ResourceWriter writer : forClass) {
                        types.addAll(writer.mediaTypes());
                        writers.add(writer);
                    }
                }
                Deque<Class<?>> interfaces = new ArrayDeque<>(Arrays.asList(c.getInterfaces()));
                while (!interfaces.isEmpty()) {
                    Class<?> iface = interfaces.poll();
                    if (seenInterfaces.contains(iface)) {
                        continue;
                    }
                    seenInterfaces.add(iface);
                    forClass = getWriters().get(iface);
                    if (forClass != null) {
                        for (ResourceWriter writer : forClass) {
                            types.addAll(writer.mediaTypes());
                            writers.add(writer);
                        }
                    }
                    interfaces.addAll(Arrays.asList(iface.getInterfaces()));
                }
                c = c.getSuperclass();
            }
            return new Holder(writers, new ArrayList<>(types));
        }
    };

    public static boolean invokeWriter(QrsRequestContext context, Object entity, MessageBodyWriter writer)
            throws IOException {
        //note that GenericEntity is not a factor here. It should have already been unwrapped

        Response response = context.getResponse();
        if (writer instanceof QrsMessageBodyWriter) {
            QrsMessageBodyWriter<Object> qrsWriter = (QrsMessageBodyWriter<Object>) writer;
            if (qrsWriter.isWriteable(entity.getClass(), context.getTarget().getLazyMethod(), context.getProducesMediaType())) {
                qrsWriter.writeResponse(entity, context);
                return true;
            } else {
                return false;
            }
        } else {
            if (writer.isWriteable(entity.getClass(), context.getGenericReturnType(), context.getAnnotations(),
                    context.getProducesMediaType())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writer.writeTo(entity, entity.getClass(), context.getGenericReturnType(),
                        context.getAnnotations(), response.getMediaType(), response.getHeaders(), baos);
                context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
                return true;
            } else {
                return false;
            }
        }
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

    public MessageBodyWriter<?>[] findWriters(QrsRequestContext context, Object entity, MediaType producesMediaType) {

        return null;
    }

    public NoMediaTypeResult findWriterNoMediaType(QrsRequestContext requestContext, Object entity) {
        Holder resultForClass = noMediaTypeClassCache.computeIfAbsent(entity.getClass(), mappingFunction);
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        List<MediaType> parsed;
        if (accept != null) {
            //TODO: this needs to be optimised
            parsed = MediaTypeHelper.parseHeader(accept);
        } else {
            //TODO: we could cache the result for no accept header, however we can't for user provided ones
            //there are an unlimited number of possible accept headers, so it would be easy to DOS if it were cached
            parsed = WILDCARD_LIST;
        }
        //TODO: more work is needed on internal default ordering
        MediaType res = MediaTypeHelper.getBestMatch(parsed, resultForClass.mediaTypeList);
        if (res == null) {
            throw new WebApplicationException(Response
                    .notAcceptable(Variant
                            .mediaTypes(
                                    resultForClass.mediaTypeList.toArray(new MediaType[resultForClass.mediaTypeList.size()]))
                            .build())
                    .build());
        }
        MediaType selected = res;
        if (res.isWildcardType() || (res.getType().equals("application") && res.isWildcardSubtype())) {
            selected = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        List<MessageBodyWriter<?>> finalResult = new ArrayList<>();
        for (ResourceWriter i : resultForClass.writers) {
            for (MediaType mt : i.mediaTypes()) {
                if (mt.isCompatible(res)) {
                    finalResult.add(i.getInstance());
                    break;
                }
            }
        }
        return new NoMediaTypeResult(finalResult.toArray(EMPTY), selected);
    }

    public static class NoMediaTypeResult {
        final MessageBodyWriter<?>[] writers;
        final MediaType mediaType;
        final EntityWriter entityWriter;

        public NoMediaTypeResult(MessageBodyWriter<?>[] writers, MediaType mediaType) {
            this.writers = writers;
            this.mediaType = mediaType;
            this.entityWriter = new FixedEntityWriterArray(writers);
        }

        public MessageBodyWriter<?>[] getWriters() {
            return writers;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public EntityWriter getEntityWriter() {
            return entityWriter;
        }
    }

    static class Holder {
        final List<ResourceWriter> writers;
        final List<MediaType> mediaTypeList;

        Holder(List<ResourceWriter> writers, List<MediaType> mediaTypeList) {
            this.writers = writers;
            this.mediaTypeList = mediaTypeList;
        }
    }
}
