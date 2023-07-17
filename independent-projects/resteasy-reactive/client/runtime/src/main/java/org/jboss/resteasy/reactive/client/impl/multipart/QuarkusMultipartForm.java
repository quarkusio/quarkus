package org.jboss.resteasy.reactive.client.impl.multipart;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.client.impl.ClientSerialisers;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

/**
 * based on {@link io.vertx.ext.web.multipart.MultipartForm} and {@link io.vertx.ext.web.multipart.impl.MultipartFormImpl}
 */
public class QuarkusMultipartForm implements Iterable<QuarkusMultipartFormDataPart> {

    private Charset charset = StandardCharsets.UTF_8;
    private final List<QuarkusMultipartFormDataPart> parts = new ArrayList<>();
    private final List<PojoFieldData> pojos = new ArrayList<>();

    public QuarkusMultipartForm setCharset(String charset) {
        return setCharset(charset != null ? Charset.forName(charset) : null);
    }

    public QuarkusMultipartForm setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public QuarkusMultipartForm attribute(String name, String value, String filename) {
        parts.add(new QuarkusMultipartFormDataPart(name, value, filename));
        return this;
    }

    public QuarkusMultipartForm entity(String name, Object entity, String mediaType, Class<?> type) {
        return entity(name, null, entity, mediaType, type);
    }

    public QuarkusMultipartForm entity(String name, String filename, Object entity, String mediaType, Class<?> type) {
        pojos.add(new PojoFieldData(name, filename, entity, mediaType, type, parts.size()));
        parts.add(null); // make place for ^
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm textFileUpload(String name, String filename, String pathname, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, pathname, mediaType, true));
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm textFileUpload(String name, String filename, Buffer content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, true));
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm stringFileUpload(String name, String filename, String content, String mediaType) {
        return textFileUpload(name, filename, Buffer.buffer(content), mediaType);
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm binaryFileUpload(String name, String filename, String pathname, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, pathname, mediaType, false));
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm binaryFileUpload(String name, String filename, Buffer content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, false));
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm multiAsBinaryFileUpload(String name, String filename, Multi<Byte> content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, false));
        return this;
    }

    @SuppressWarnings("unused")
    public QuarkusMultipartForm multiAsTextFileUpload(String name, String filename, Multi<Byte> content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, true));
        return this;
    }

    @Override
    public Iterator<QuarkusMultipartFormDataPart> iterator() {
        return parts.iterator();
    }

    public void preparePojos(RestClientRequestContext context) throws IOException {
        Serialisers serialisers = context.getRestClient().getClientContext().getSerialisers();
        for (PojoFieldData pojo : pojos) {
            MultivaluedMap<String, String> headers = new MultivaluedTreeMap<>();

            Object entityObject = pojo.entity;
            Entity<?> entity = Entity.entity(entityObject, pojo.mediaType);
            Class<?> entityClass;
            Type entityType;
            if (entityObject instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) entityObject;
                entityClass = genericEntity.getRawType();
                entityType = pojo.type;
                entityObject = genericEntity.getEntity();
            } else {
                entityType = entityClass = pojo.type;
            }

            List<MessageBodyWriter<?>> writers = serialisers.findWriters(context.getConfiguration(),
                    entityClass, entity.getMediaType(),
                    RuntimeType.CLIENT);
            Buffer value = null;
            for (MessageBodyWriter<?> w : writers) {
                Buffer ret = ClientSerialisers.invokeClientWriter(entity, entityObject, entityClass, entityType, headers, w,
                        context.getConfiguration().getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR),
                        context.getProperties(), context, serialisers, context.getConfiguration());
                if (ret != null) {
                    value = ret;
                    break;
                }
            }
            parts.set(pojo.position,
                    new QuarkusMultipartFormDataPart(pojo.name, pojo.filename, value, pojo.mediaType, pojo.type));
        }
    }

    public static class PojoFieldData {
        private final String name;
        private final String filename;
        private final Object entity;
        private final String mediaType;
        private final Class<?> type;
        private final int position;

        public PojoFieldData(String name, String filename, Object entity, String mediaType, Class<?> type, int position) {
            this.name = name;
            this.filename = filename;
            this.entity = entity;
            this.mediaType = mediaType;
            this.type = type;
            this.position = position;
        }
    }
}
