package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.providers.serialisers.ClientDefaultTextPlainBodyHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.core.UnmanagedBeanFactory;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.providers.serialisers.BooleanMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.ByteArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.CharArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.CharacterMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.FormUrlEncodedProvider;
import org.jboss.resteasy.reactive.common.providers.serialisers.InputStreamMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.NumberMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.ReaderBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.StringMessageBodyHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonArrayHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonObjectHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonValueHandler;

public class ClientSerialisers extends Serialisers {

    private static final Logger log = Logger.getLogger(ClientSerialisers.class);

    public static BuiltinReader[] BUILTIN_READERS = new BuiltinReader[] {
            new BuiltinReader(String.class, StringMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinReader(Boolean.class, BooleanMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(Character.class, CharacterMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(Number.class, NumberMessageBodyHandler.class,
                    MediaType.TEXT_PLAIN),
            new BuiltinReader(InputStream.class, InputStreamMessageBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(Reader.class, ReaderBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(File.class, FileBodyHandler.class, MediaType.WILDCARD),

            new BuiltinReader(byte[].class, ByteArrayMessageBodyHandler.class, MediaType.WILDCARD),
            new BuiltinReader(MultivaluedMap.class, FormUrlEncodedProvider.class, MediaType.APPLICATION_FORM_URLENCODED,
                    RuntimeType.CLIENT),
            new BuiltinReader(Object.class, ClientDefaultTextPlainBodyHandler.class, MediaType.TEXT_PLAIN, RuntimeType.CLIENT),
            new BuiltinReader(JsonArray.class, JsonArrayHandler.class, MediaType.APPLICATION_JSON, RuntimeType.CLIENT),
            new BuiltinReader(JsonObject.class, JsonObjectHandler.class, MediaType.APPLICATION_JSON, RuntimeType.CLIENT),
            new BuiltinReader(JsonValue.class, JsonValueHandler.class, MediaType.APPLICATION_JSON, RuntimeType.CLIENT)
    };
    public static BuiltinWriter[] BUILTIN_WRITERS = new BuiltinWriter[] {
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
            //            new BuiltinWriter(Buffer.class, VertxBufferMessageBodyWriter.class,
            //                    MediaType.WILDCARD),
            new BuiltinWriter(MultivaluedMap.class, FormUrlEncodedProvider.class,
                    MediaType.APPLICATION_FORM_URLENCODED),
            new BuiltinWriter(InputStream.class, InputStreamMessageBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(Reader.class, ReaderBodyHandler.class,
                    MediaType.WILDCARD),
            new BuiltinWriter(File.class, FileBodyHandler.class,
                    MediaType.WILDCARD),
    };

    // FIXME: pass InvocationState to wrap args?
    public static Buffer invokeClientWriter(Entity<?> entity, Object entityObject, Class<?> entityClass, Type entityType,
            MultivaluedMap<String, String> headerMap, MessageBodyWriter writer, WriterInterceptor[] writerInterceptors,
            Map<String, Object> properties, Serialisers serialisers, ConfigurationImpl configuration)
            throws IOException {

        if (writer.isWriteable(entityClass, entityType, entity.getAnnotations(), entity.getMediaType())) {
            if (writerInterceptors == null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writer.writeTo(entityObject, entityClass, entityType, entity.getAnnotations(),
                        entity.getMediaType(), headerMap, baos);
                return Buffer.buffer(baos.toByteArray());
            } else {
                return runClientWriterInterceptors(entityObject, entityClass, entityType, entity.getAnnotations(),
                        entity.getMediaType(), headerMap, writer, writerInterceptors, properties, serialisers,
                        configuration);
            }
        }

        return null;
    }

    public static Buffer runClientWriterInterceptors(Object entity, Class<?> entityClass, Type entityType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers, MessageBodyWriter writer,
            WriterInterceptor[] writerInterceptors, Map<String, Object> properties, Serialisers serialisers,
            ConfigurationImpl configuration) throws IOException {
        ClientWriterInterceptorContextImpl wc = new ClientWriterInterceptorContextImpl(writerInterceptors, writer,
                annotations, entityClass, entityType, entity, mediaType, headers, properties, serialisers, configuration);
        wc.proceed();
        return wc.getResult();
    }

    public static Object invokeClientReader(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType, Map<String, Object> properties,
            MultivaluedMap metadata, Serialisers serialisers, InputStream in, ReaderInterceptor[] interceptors,
            ConfigurationImpl configuration)
            throws WebApplicationException, IOException {
        // FIXME: perhaps optimise for when we have no interceptor?
        ClientReaderInterceptorContextImpl context = new ClientReaderInterceptorContextImpl(annotations,
                entityClass, entityType, mediaType,
                properties, metadata, configuration, serialisers, in, interceptors);
        return context.proceed();
    }

    public BuiltinWriter[] getBuiltinWriters() {
        return BUILTIN_WRITERS;
    }

    public BuiltinReader[] getBuiltinReaders() {
        return BUILTIN_READERS;
    }

    public void registerBuiltins(RuntimeType constraint) {
        for (BuiltinWriter builtinWriter : getBuiltinWriters()) {
            if (builtinWriter.constraint == null || builtinWriter.constraint == constraint) {
                MessageBodyWriter<?> writer;
                try {
                    writer = builtinWriter.writerClass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException e) {
                    log.error("Unable to instantiate MessageBodyWriter", e);
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
        for (BuiltinReader builtinReader : getBuiltinReaders()) {
            if (builtinReader.constraint == null || builtinReader.constraint == constraint) {
                MessageBodyReader<?> reader;
                try {
                    reader = builtinReader.readerClass.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException e) {
                    log.error("Unable to instantiate MessageBodyReader", e);
                    continue;
                }
                ResourceReader resourceReader = new ResourceReader();
                resourceReader.setConstraint(builtinReader.constraint);
                resourceReader.setMediaTypeStrings(Collections.singletonList(builtinReader.mediaType));
                // FIXME: we could still support beans
                resourceReader.setFactory(new UnmanagedBeanFactory<MessageBodyReader<?>>(reader));
                addReader(builtinReader.entityClass, resourceReader);
            }
        }
    }
}
