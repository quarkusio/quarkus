package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.EntityPartImpl;
import org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultipartParser;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

@Consumes("multipart/form-data")
public class EntityPartReader implements MessageBodyReader<List<EntityPart>> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (!List.class.isAssignableFrom(type)) {
            return false;
        }
        if (mediaType == null || !mediaType.getType().equals("multipart")) {
            return false;
        }
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            return args.length == 1 && args[0] == EntityPart.class;
        }
        return false;
    }

    @Override
    public List<EntityPart> readFrom(Class<List<EntityPart>> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String boundary = mediaType.getParameters().get("boundary");
        if (boundary == null) {
            throw new WebApplicationException("Missing boundary parameter in Content-Type");
        }
        byte[] body = entityStream.readAllBytes();

        List<EntityPart> parts = new ArrayList<>();
        EntityPartCollector collector = new EntityPartCollector(parts);
        MultipartParser.ParseState parser = MultipartParser.beginParse(
                collector, boundary.getBytes(StandardCharsets.US_ASCII), "UTF-8");
        parser.parse(ByteBuffer.wrap(body));
        return parts;
    }

    private static class EntityPartCollector implements MultipartParser.PartHandler {
        private final List<EntityPart> parts;
        private CaseInsensitiveMap<String> currentHeaders;
        private ByteArrayOutputStream currentData;

        EntityPartCollector(List<EntityPart> parts) {
            this.parts = parts;
        }

        @Override
        public void beginPart(CaseInsensitiveMap<String> headers) {
            this.currentHeaders = headers;
            this.currentData = new ByteArrayOutputStream();
        }

        @Override
        public void data(ByteBuffer buffer) throws IOException {
            if (buffer.hasArray()) {
                currentData.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            } else {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                currentData.write(bytes);
            }
        }

        @Override
        public void endPart() {
            String contentDisposition = currentHeaders.getFirst("Content-Disposition");
            if (contentDisposition == null) {
                return;
            }
            String name = extractParam(contentDisposition, "name");
            if (name == null) {
                return;
            }
            String fileName = extractParam(contentDisposition, "filename");

            MediaType partMediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            String contentType = currentHeaders.getFirst("Content-Type");
            if (contentType != null) {
                partMediaType = MediaType.valueOf(contentType);
            }

            MultivaluedMap<String, String> headers = new QuarkusMultivaluedHashMap<>();
            for (var entry : currentHeaders.entrySet()) {
                headers.put(entry.getKey(), entry.getValue());
            }

            Serialisers serialisers = ((RuntimeDelegateImpl) RuntimeDelegate.getInstance()).getSerialisers();
            parts.add(new EntityPartImpl(name, fileName, headers, partMediaType,
                    new ByteArrayInputStream(currentData.toByteArray()), serialisers));
        }

        private static String extractParam(String header, String paramName) {
            String search = paramName + "=\"";
            int idx = header.indexOf(search);
            if (idx < 0) {
                return null;
            }
            int start = idx + search.length();
            int end = header.indexOf('"', start);
            if (end < 0) {
                return null;
            }
            return header.substring(start, end);
        }
    }
}
