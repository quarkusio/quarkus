package io.quarkus.resteasy.reactive.server.runtime.multipart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class MultipartMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private static final String RESTEASY_DEFAULT_CHARSET_PROPERTY = "quarkus.resteasy-reactive.multipart.input-part.default-charset";
    private static final Charset RESTEASY_DEFAULT_CHARSET_PROPERTY_DEFAULT = StandardCharsets.UTF_8;
    private static final String DOUBLE_DASH = "--";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String BOUNDARY_PARAM = "boundary";

    private final Charset defaultCharset;

    public MultipartMessageBodyWriter() {
        defaultCharset = ConfigProvider.getConfig().getOptionalValue(RESTEASY_DEFAULT_CHARSET_PROPERTY, Charset.class)
                .orElse(RESTEASY_DEFAULT_CHARSET_PROPERTY_DEFAULT);
    }

    @Override
    public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream)
            throws IOException, WebApplicationException {
        writeMultiformData(o, mediaType, outputStream);
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        writeMultiformData(o, context.getResponseMediaType(), context.getOrCreateOutputStream());
    }

    public static final String getGeneratedMapperClassNameFor(String className) {
        return className + "_generated_mapper";
    }

    private void writeMultiformData(Object o, MediaType mediaType, OutputStream outputStream) throws IOException {
        ResteasyReactiveRequestContext requestContext = CurrentRequestManager.get();

        String boundary = generateBoundary();
        appendBoundaryIntoMediaType(requestContext, boundary, mediaType);
        List<PartItem> formData = toFormData(o);
        write(formData, boundary, outputStream, requestContext);
    }

    private List<PartItem> toFormData(Object o) {
        String transformer = getGeneratedMapperClassNameFor(o.getClass().getName());
        BeanFactory.BeanInstance instance = new ReflectionBeanFactoryCreator().apply(transformer).createInstance();
        return ((MultipartOutputInjectionTarget) instance.getInstance()).mapFrom(o);
    }

    private void write(List<PartItem> parts, String boundary, OutputStream outputStream,
            ResteasyReactiveRequestContext requestContext)
            throws IOException {
        String boundaryLine = "--" + boundary;
        for (PartItem part : parts) {
            // write boundary: --...
            writeLine(outputStream, boundaryLine);
            // write content disposition header
            writeLine(outputStream, HttpHeaders.CONTENT_DISPOSITION + ": form-data; name=\"" + part.getName() + "\""
                    + getFileNameIfFile(part.getValue()));
            // write content content type
            writeLine(outputStream, HttpHeaders.CONTENT_TYPE + ": " + part.getType());
            // extra line
            writeLine(outputStream);

            // write content
            write(outputStream, serialiseEntity(part.getValue(), part.getType(), requestContext));
            // extra line
            writeLine(outputStream);
        }

        // write boundary: -- ... --
        write(outputStream, boundaryLine + DOUBLE_DASH);
    }

    private String getFileNameIfFile(Object value) {
        if (value instanceof File) {
            return "; filename=\"" + ((File) value).getName() + "\"";
        }

        return "";
    }

    private void writeLine(OutputStream os, String text) throws IOException {
        write(os, text);
        writeLine(os);
    }

    private void write(OutputStream os, String text) throws IOException {
        write(os, text.getBytes(defaultCharset));
    }

    private void write(OutputStream os, byte[] bytes) throws IOException {
        os.write(bytes);
    }

    private void writeLine(OutputStream os) throws IOException {
        os.write(LINE_SEPARATOR.getBytes(defaultCharset));
    }

    private byte[] serialiseEntity(Object entity, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws IOException {
        ServerSerialisers serializers = context.getDeployment().getSerialisers();
        Class<?> entityClass = entity.getClass();
        Type entityType = null;
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serializers
                .findWriters(null, entityClass, mediaType, RuntimeType.SERVER)
                .toArray(ServerSerialisers.NO_WRITER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean wrote = false;
        for (MessageBodyWriter<Object> writer : writers) {
            if (writer.isWriteable(entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType)) {
                // FIXME: spec doesn't really say what headers we should use here
                writer.writeTo(entity, entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType,
                        Serialisers.EMPTY_MULTI_MAP, baos);
                wrote = true;
                break;
            }
        }

        if (!wrote) {
            throw new IllegalStateException("Could not find MessageBodyWriter for " + entityClass + " as " + mediaType);
        }
        return baos.toByteArray();
    }

    private String generateBoundary() {
        return UUID.randomUUID().toString();
    }

    private void appendBoundaryIntoMediaType(ResteasyReactiveRequestContext requestContext, String boundary,
            MediaType mediaType) {
        requestContext.setResponseContentType(new MediaType(mediaType.getType(), mediaType.getSubtype(),
                Collections.singletonMap(BOUNDARY_PARAM, boundary)));
    }
}
