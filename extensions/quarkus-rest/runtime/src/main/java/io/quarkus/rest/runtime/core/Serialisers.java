package io.quarkus.rest.runtime.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.runtime.client.QuarkusRestClientReaderInterceptorContext;
import io.quarkus.rest.runtime.client.QuarkusRestClientWriterInterceptorContext;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestWriterInterceptorContext;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.providers.serialisers.ByteArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.CharArrayMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.FileBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.FormUrlEncodedProvider;
import io.quarkus.rest.runtime.providers.serialisers.InputStreamMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.ReaderBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.StringMessageBodyHandler;
import io.quarkus.rest.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.quarkus.rest.runtime.providers.serialisers.VertxJsonMessageBodyWriter;
import io.quarkus.rest.runtime.providers.serialisers.jsonb.JsonbMessageBodyReader;
import io.quarkus.rest.runtime.spi.QuarkusRestClientMessageBodyWriter;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.quarkus.rest.runtime.util.MediaTypeHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class Serialisers {

    public static class Builtin {
        public final Class<?> entityClass;
        public final String mediaType;
        public final RuntimeType constraint;

        public Builtin(Class<?> entityClass, String mediaType, RuntimeType constraint) {
            this.entityClass = entityClass;
            this.mediaType = mediaType;
            this.constraint = constraint;
        }
    }

    public static class BuiltinWriter extends Builtin {
        public final Class<? extends MessageBodyWriter<?>> writerClass;

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType) {
            this(entityClass, writerClass, mediaType, null);
        }

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.writerClass = writerClass;
        }
    }

    public static class BuiltinReader extends Builtin {
        public final Class<? extends MessageBodyReader<?>> readerClass;

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType) {
            this(entityClass, readerClass, mediaType, null);
        }

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.readerClass = readerClass;
        }
    }

    public static BuiltinReader[] BUILTIN_READERS = new BuiltinReader[] {
            new BuiltinReader(String.class, StringMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinReader(InputStream.class, InputStreamMessageBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(Reader.class, ReaderBodyHandler.class, MediaType.TEXT_PLAIN),
            new BuiltinReader(File.class, FileBodyHandler.class, MediaType.WILDCARD),

            // the client always expects these to exist
            new BuiltinReader(byte[].class, ByteArrayMessageBodyHandler.class, MediaType.WILDCARD, RuntimeType.CLIENT),
            new BuiltinReader(MultivaluedMap.class, FormUrlEncodedProvider.class, MediaType.APPLICATION_FORM_URLENCODED,
                    RuntimeType.CLIENT),

            //TODO: Do the Jsonb readers always make sense?
            new BuiltinReader(Object.class, JsonbMessageBodyReader.class,
                    MediaType.APPLICATION_JSON),
    };

    public static BuiltinWriter[] BUILTIN_WRITERS = new BuiltinWriter[] {
            new BuiltinWriter(Object.class, VertxJsonMessageBodyWriter.class,
                    MediaType.APPLICATION_JSON),
            new BuiltinWriter(String.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Number.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Boolean.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Character.class, StringMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(Object.class, StringMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(char[].class, CharArrayMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(byte[].class, ByteArrayMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(Buffer.class, VertxBufferMessageBodyWriter.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(MultivaluedMap.class, FormUrlEncodedProvider.class,
                    MediaType.APPLICATION_FORM_URLENCODED),
            new BuiltinWriter(InputStream.class, InputStreamMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(Reader.class, ReaderBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinWriter(File.class, FileBodyHandler.class,
                    MediaType.WILDCARD),
    };

    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection 
    private final MultivaluedMap<Class<?>, ResourceWriter> writers = new MultivaluedHashMap<>();
    private final MultivaluedMap<Class<?>, ResourceReader> readers = new MultivaluedHashMap<>();

    public static final List<MediaType> WILDCARD_LIST = Collections.singletonList(MediaType.WILDCARD_TYPE);
    public static final List<String> WILDCARD_STRING_LIST = Collections.singletonList(MediaType.WILDCARD);

    public static final MessageBodyWriter<?>[] NO_WRITER = new MessageBodyWriter[0];
    public static final MessageBodyReader<?>[] NO_READER = new MessageBodyReader[0];
    public static final WriterInterceptor[] NO_WRITER_INTERCEPTOR = new WriterInterceptor[0];
    public static final ReaderInterceptor[] NO_READER_INTERCEPTOR = new ReaderInterceptor[0];
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
                    for (Class<?> i : iface.getInterfaces()) {
                        interfaces.add(i);
                    }
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
            if (writer.isWriteable(entity.getClass(), context.getGenericReturnType(), context.getAllAnnotations(),
                    context.getProducesMediaType())) {
                if (mediaType != null) {
                    context.setProducesMediaType(mediaType);
                }
                if (writerInterceptors == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entity, entity.getClass(), context.getGenericReturnType(),
                            context.getAllAnnotations(), response.getMediaType(), response.getHeaders(), baos);
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
                context.getAllAnnotations(), entity.getClass(), context.getGenericReturnType(), entity, response.getMediaType(),
                response.getHeaders());
        wc.proceed();

    }

    public void registerBuiltins(RuntimeType constraint) {
        for (BuiltinWriter builtinWriter : BUILTIN_WRITERS) {
            if (builtinWriter.constraint == null || builtinWriter.constraint == constraint) {
                MessageBodyWriter<?> writer;
                try {
                    writer = builtinWriter.writerClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                ResourceWriter resourceWriter = new ResourceWriter();
                resourceWriter.setConstraint(builtinWriter.constraint);
                resourceWriter.setMediaTypeStrings(Collections.singletonList(builtinWriter.mediaType));
                // FIXME: we could still support beans
                resourceWriter.setFactory(new UnmanagedBeanFactory<MessageBodyWriter<?>>(writer));
                addWriter(builtinWriter.entityClass, resourceWriter);
            }
        }
        for (BuiltinReader builtinReader : BUILTIN_READERS) {
            if (builtinReader.constraint == null || builtinReader.constraint == constraint) {
                MessageBodyReader<?> reader;
                try {
                    reader = builtinReader.readerClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                ResourceReader resourceWriter = new ResourceReader();
                resourceWriter.setConstraint(builtinReader.constraint);
                resourceWriter.setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType));
                // FIXME: we could still support beans
                resourceWriter.setFactory(new UnmanagedBeanFactory<MessageBodyReader<?>>(reader));
                addReader(builtinReader.entityClass, resourceWriter);
            }
        }
    }

    public List<MessageBodyReader<?>> findReaders(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType mediaType) {
        return findReaders(configuration, entityType, mediaType, null);
    }

    public List<MessageBodyReader<?>> findReaders(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType mediaType, RuntimeType runtimeType) {
        List<MediaType> mt = Collections.singletonList(mediaType);
        List<MessageBodyReader<?>> ret = new ArrayList<>();
        Deque<Class<?>> toProcess = new LinkedList<>();
        Class<?> klass = entityType;
        MultivaluedMap<Class<?>, ResourceReader> readers;
        if (configuration != null && !configuration.getResourceReaders().isEmpty()) {
            readers = new MultivaluedHashMap<>();
            readers.putAll(this.readers);
            readers.putAll(configuration.getResourceReaders());
        } else {
            readers = this.readers;
        }
        do {
            for (Class<?> i : klass.getInterfaces()) {
                toProcess.add(i);
            }
            if (klass == Object.class || klass.getSuperclass() == null) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceReader> goodTypeReaders = readers.get(iface);
                    readerLookup(mediaType, runtimeType, mt, ret, goodTypeReaders);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceReader> goodTypeReaders = readers.get(klass);
            readerLookup(mediaType, runtimeType, mt, ret, goodTypeReaders);
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    private void readerLookup(MediaType mediaType, RuntimeType runtimeType, List<MediaType> mt, List<MessageBodyReader<?>> ret,
            List<ResourceReader> goodTypeReaders) {
        if (goodTypeReaders != null && !goodTypeReaders.isEmpty()) {
            for (ResourceReader goodTypeReader : goodTypeReaders) {
                if (!goodTypeReader.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeReader.mediaTypes());
                if (match != null || mediaType == null) {
                    ret.add(goodTypeReader.getInstance());
                }
            }
        }
    }

    public <T> void addWriter(Class<T> entityClass, ResourceWriter writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader reader) {
        readers.add(entityClass, reader);
    }

    public List<ResourceWriter> findBuildTimeWriters(Class<?> entityType, RuntimeType runtimeType, String... produces) {
        List<MediaType> type = new ArrayList<>();
        for (String i : produces) {
            type.add(MediaType.valueOf(i));
        }
        return findBuildTimeWriters(entityType, runtimeType, type);
    }

    private List<ResourceWriter> findBuildTimeWriters(Class<?> entityType, RuntimeType runtimeType, List<MediaType> produces) {
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
                    if (!goodTypeWriter.matchesRuntimeType(runtimeType)) {
                        continue;
                    }
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

    public List<MessageBodyWriter<?>> findWriters(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType resolvedMediaType) {
        return findWriters(configuration, entityType, resolvedMediaType, null);
    }

    public List<MessageBodyWriter<?>> findWriters(QuarkusRestConfiguration configuration, Class<?> entityType,
            MediaType resolvedMediaType, RuntimeType runtimeType) {
        // FIXME: invocation is very different between client and server, where the server doesn't treat GenericEntity specially
        // it's probably missing from there, while the client handles it upstack
        List<MediaType> mt = Collections.singletonList(resolvedMediaType);
        List<MessageBodyWriter<?>> ret = new ArrayList<>();
        List<MessageBodyWriter<?>> objectMatched = new ArrayList<>();
        Class<?> klass = entityType;
        Deque<Class<?>> toProcess = new LinkedList<>();
        MultivaluedMap<Class<?>, ResourceWriter> writers;
        if (configuration != null && !configuration.getResourceWriters().isEmpty()) {
            writers = new MultivaluedHashMap<>();
            writers.putAll(this.writers);
            writers.putAll(configuration.getResourceWriters());
        } else {
            writers = this.writers;
        }

        do {
            if (klass == Object.class) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceWriter> goodTypeWriters = writers.get(iface);
                    writerLookup(runtimeType, mt, ret, goodTypeWriters);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceWriter> goodTypeWriters = writers.get(klass);
            writerLookup(runtimeType, mt, ret, goodTypeWriters);
            toProcess.addAll(Arrays.asList(klass.getInterfaces()));
            klass = klass.getSuperclass();
        } while (klass != null);

        return ret;
    }

    private void writerLookup(RuntimeType runtimeType, List<MediaType> mt, List<MessageBodyWriter<?>> ret,
            List<ResourceWriter> goodTypeWriters) {
        if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
            for (ResourceWriter goodTypeWriter : goodTypeWriters) {
                if (!goodTypeWriter.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getBestMatch(mt, goodTypeWriter.mediaTypes());
                if (match != null) {
                    ret.add(goodTypeWriter.getInstance());
                }
            }
        }
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

    // FIXME: pass InvocationState to wrap args?
    public static Buffer invokeClientWriter(Entity<?> entity, Object entityObject, Class<?> entityClass, Type entityType,
            MultivaluedMap<String, String> headerMap, MessageBodyWriter writer, WriterInterceptor[] writerInterceptors,
            Map<String, Object> properties) throws IOException {
        if (writer instanceof QuarkusRestClientMessageBodyWriter && writerInterceptors == null) {
            QuarkusRestClientMessageBodyWriter<Object> quarkusRestWriter = (QuarkusRestClientMessageBodyWriter<Object>) writer;
            if (writer.isWriteable(entityClass, entityType, entity.getAnnotations(), entity.getMediaType())) {
                return quarkusRestWriter.writeResponse(entityObject);
            }
        } else {
            if (writer.isWriteable(entityClass, entityType, entity.getAnnotations(), entity.getMediaType())) {
                if (writerInterceptors == null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entityObject, entityClass, entityType, entity.getAnnotations(),
                            entity.getMediaType(), headerMap, baos);
                    return Buffer.buffer(baos.toByteArray());
                } else {
                    return runClientWriterInterceptors(entityObject, entityClass, entityType, entity.getAnnotations(),
                            entity.getMediaType(), headerMap, writer, writerInterceptors, properties);
                }
            }
        }

        return null;
    }

    public static Buffer runClientWriterInterceptors(Object entity, Class<?> entityClass, Type entityType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers, MessageBodyWriter writer,
            WriterInterceptor[] writerInterceptors, Map<String, Object> properties) throws IOException {
        QuarkusRestClientWriterInterceptorContext wc = new QuarkusRestClientWriterInterceptorContext(writerInterceptors, writer,
                annotations, entityClass, entityType, entity,
                mediaType,
                headers, properties);
        wc.proceed();
        return wc.getResult();
    }

    public static Object invokeClientReader(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType, Map<String, Object> properties,
            MultivaluedMap metadata, MessageBodyReader<?> reader, InputStream in, ReaderInterceptor[] interceptors)
            throws WebApplicationException, IOException {
        // FIXME: perhaps optimise for when we have no interceptor?
        QuarkusRestClientReaderInterceptorContext context = new QuarkusRestClientReaderInterceptorContext(annotations,
                entityClass, entityType, mediaType,
                properties, (MultivaluedMap) metadata, reader, in, interceptors);
        return context.proceed();
    }
}
