package io.quarkus.rest.runtime.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
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
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestWriterInterceptorContext;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.quarkus.rest.runtime.util.MediaTypeHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class Serialisers {

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private final MultivaluedMap<Class<?>, ResourceWriter> writers = new MultivaluedHashMap<>();
    private final MultivaluedMap<Class<?>, ResourceReader> readers = new MultivaluedHashMap<>();

    public static final List<MediaType> WILDCARD_LIST = Collections.singletonList(MediaType.WILDCARD_TYPE);

    public static final MessageBodyWriter<?>[] NO_WRITER = new MessageBodyWriter[0];
    public static final MessageBodyReader<?>[] NO_READER = new MessageBodyReader[0];
    public static final Annotation[] NO_ANNOTATION = new Annotation[0];
    public static final MultivaluedMap<String, Object> EMPTY_MULTI_MAP = new MultivaluedHashMap<>();

    private final ConcurrentMap<Class<?>, List<ResourceWriter>> noMediaTypeClassCache = new ConcurrentHashMap<>();
    private final Function<Class<?>, List<ResourceWriter>> mappingFunction = new Function<Class<?>, List<ResourceWriter>>() {
        @Override
        public List<ResourceWriter> apply(Class<?> aClass) {
            Class<?> c = aClass;
            List<ResourceWriter> writers = new ArrayList<>();
            Set<Class<?>> seenInterfaces = new HashSet<>();
            while (c != null) {
                List<ResourceWriter> forClass = getWriters().get(c);
                if (forClass != null) {
                    writers.addAll(forClass);
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
                        writers.addAll(forClass);
                    }
                    interfaces.addAll(Arrays.asList(iface.getInterfaces()));
                }
                c = c.getSuperclass();
            }
            return writers;
        }
    };

    public static boolean invokeWriter(QuarkusRestRequestContext context, Object entity, MessageBodyWriter writer)
            throws IOException {
        return invokeWriter(context, entity, writer, null);
    }

    public static boolean invokeWriter(QuarkusRestRequestContext context, Object entity, MessageBodyWriter writer,
            MediaType mediaType)
            throws IOException {
        //note that GenericEntity is not a factor here. It should have already been unwrapped

        Response response = context.getResponse();
        WriterInterceptor[] writerInterceptors = context.getWriterInterceptors();
        if (writer instanceof QuarkusRestMessageBodyWriter && writerInterceptors == null) {
            QuarkusRestMessageBodyWriter<Object> quarkusRestWriter = (QuarkusRestMessageBodyWriter<Object>) writer;
            RuntimeResource target = context.getTarget();
            Serialisers.encodeResponseHeaders(context);
            if (quarkusRestWriter.isWriteable(entity.getClass(), target == null ? null : target.getLazyMethod(),
                    context.getProducesMediaType())) {
                if (mediaType != null) {
                    context.setProducesMediaType(mediaType);
                }
                quarkusRestWriter.writeResponse(entity, context);
                return true;
            } else {
                return false;
            }
        } else {
            if (writer.isWriteable(entity.getClass(), context.getGenericReturnType(), context.getAnnotations(),
                    context.getProducesMediaType())) {
                if (mediaType != null) {
                    context.setProducesMediaType(mediaType);
                }
                if (writerInterceptors == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entity, entity.getClass(), context.getGenericReturnType(),
                            context.getAnnotations(), response.getMediaType(), response.getHeaders(), baos);
                    Serialisers.encodeResponseHeaders(context);
                    context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
                } else {
                    runWriterInterceptors(context, entity, writer, response, writerInterceptors);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static void runWriterInterceptors(QuarkusRestRequestContext context, Object entity, MessageBodyWriter writer,
            Response response, WriterInterceptor[] writerInterceptor) throws IOException {
        QuarkusRestWriterInterceptorContext wc = new QuarkusRestWriterInterceptorContext(context, writerInterceptor, writer,
                context.getAnnotations(), entity.getClass(), context.getGenericReturnType(), entity, response.getMediaType(),
                response.getHeaders());
        wc.proceed();

    }

    public List<MessageBodyReader<?>> findReaders(Class<?> entityType, MediaType mediaType) {
        List<MediaType> mt = Collections.singletonList(mediaType);
        List<MessageBodyReader<?>> ret = new ArrayList<>();
        Class<?> klass = entityType;
        do {
            List<ResourceReader> goodTypeReaders = readers.get(klass);
            if (goodTypeReaders != null && !goodTypeReaders.isEmpty()) {
                for (ResourceReader goodTypeReader : goodTypeReaders) {
                    MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeReader.mediaTypes());
                    if (match != null || mediaType == null) {
                        ret.add(goodTypeReader.getInstance());
                    }
                }
            }
            // FIXME: spec mentions superclasses, but surely interfaces are involved too?
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    public <T> void addWriter(Class<T> entityClass, ResourceWriter writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader reader) {
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

    public MultivaluedMap<Class<?>, ResourceReader> getReaders() {
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

    public NoMediaTypeResult findWriterNoMediaType(QuarkusRestRequestContext requestContext, Object entity) {
        List<ResourceWriter> resultForClass = noMediaTypeClassCache.computeIfAbsent(entity.getClass(), mappingFunction);
        //TODO: more work is needed on internal default ordering
        MediaType selected = null;
        for (ResourceWriter writer : resultForClass) {
            selected = writer.serverMediaType().negotiateProduces(requestContext.getContext().request());
            if (selected != null) {
                break;
            }
        }
        if (selected == null) {
            Set<MediaType> acceptable = new HashSet<>();
            for (ResourceWriter i : resultForClass) {
                acceptable.addAll(i.mediaTypes());
            }

            throw new WebApplicationException(Response
                    .notAcceptable(Variant
                            .mediaTypes(
                                    acceptable.toArray(new MediaType[0]))
                            .build())
                    .build());
        }
        if (selected.isWildcardType() || (selected.getType().equals("application") && selected.isWildcardSubtype())) {
            selected = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        List<MessageBodyWriter<?>> finalResult = new ArrayList<>(resultForClass.size());
        for (ResourceWriter i : resultForClass) {
            // this part seems to be needed in order to pass com.sun.ts.tests.jaxrs.ee.resource.java2entity.JAXRSClient
            if (i.mediaTypes().isEmpty()) {
                finalResult.add(i.getInstance());
            } else {
                for (MediaType mt : i.mediaTypes()) {
                    if (mt.isCompatible(selected)) {
                        finalResult.add(i.getInstance());
                        break;
                    }
                }
            }
        }
        return new NoMediaTypeResult(finalResult.toArray(NO_WRITER), selected);
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

    public static void encodeResponseHeaders(QuarkusRestRequestContext requestContext) {
        HttpServerResponse vertxResponse = requestContext.getContext().response();
        Response response = requestContext.getResponse();
        vertxResponse.setStatusCode(response.getStatus());
        if (response.getStatusInfo().getReasonPhrase() != null) {
            vertxResponse.setStatusMessage(response.getStatusInfo().getReasonPhrase());
        }
        MultivaluedMap<String, String> headers = response.getStringHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getValue().size() == 1) {
                vertxResponse.putHeader(entry.getKey(), entry.getValue().get(0));
            } else {
                vertxResponse.putHeader(entry.getKey(), entry.getValue());
            }
        }
    }
}
