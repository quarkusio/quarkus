package org.jboss.resteasy.reactive.server.core.multipart;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.reflection.ReflectionBeanFactoryCreator;
import org.jboss.resteasy.reactive.multipart.FileDownload;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class MultipartMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    private static final String DOUBLE_DASH = "--";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String BOUNDARY_PARAM = "boundary";

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
        Charset charset = requestContext.getDeployment().getRuntimeConfiguration().body().defaultCharset();
        String boundaryLine = "--" + boundary;
        for (PartItem part : parts) {
            Object partValue = part.getValue();
            if (partValue != null) {
                if (isListOf(part, File.class) || isListOf(part, FileDownload.class)) {
                    List<Object> list = (List<Object>) part.getValue();
                    for (int i = 0; i < list.size(); i++) {
                        writePart(part.getName(), list.get(i), part.getType(),
                                boundaryLine, charset, outputStream, requestContext);
                    }
                } else {
                    writePart(part.getName(), part.getValue(), part.getType(),
                            boundaryLine, charset, outputStream, requestContext);
                }
            }
        }

        // write boundary: -- ... --
        write(outputStream, boundaryLine + DOUBLE_DASH, charset);
    }

    private void writePart(String partName,
            Object partValue,
            MediaType partType,
            String boundaryLine,
            Charset charset,
            OutputStream outputStream,
            ResteasyReactiveRequestContext requestContext) throws IOException {

        if (partValue instanceof FileDownload) {
            FileDownload fileDownload = (FileDownload) partValue;
            partValue = fileDownload.filePath().toFile();
            // overwrite properties if set
            partName = isNotEmpty(fileDownload.name()) ? fileDownload.name() : partName;
            partType = isNotEmpty(fileDownload.contentType()) ? MediaType.valueOf(fileDownload.contentType()) : partType;
            charset = isNotEmpty(fileDownload.charSet()) ? Charset.forName(fileDownload.charSet()) : charset;
        }

        // write boundary: --...
        writeLine(outputStream, boundaryLine, charset);
        // write content disposition header
        writeLine(outputStream, HttpHeaders.CONTENT_DISPOSITION + ": form-data; name=\"" + partName + "\""
                + getFileNameIfFile(partValue), charset);
        // write content content type
        writeLine(outputStream, HttpHeaders.CONTENT_TYPE + ": " + partType, charset);
        // extra line
        writeLine(outputStream, charset);

        // write content
        writeEntity(outputStream, partValue, partType, requestContext);
        // extra line
        writeLine(outputStream, charset);
    }

    private String getFileNameIfFile(Object value) {
        if (value instanceof File) {
            return "; filename=\"" + ((File) value).getName() + "\"";
        } else if (value instanceof FileDownload) {
            return "; filename=\"" + ((FileDownload) value).fileName() + "\"";
        }

        return "";
    }

    private void writeLine(OutputStream os, String text, Charset defaultCharset) throws IOException {
        write(os, text, defaultCharset);
        writeLine(os, defaultCharset);
    }

    private void write(OutputStream os, String text, Charset defaultCharset) throws IOException {
        write(os, text.getBytes(defaultCharset));
    }

    private void write(OutputStream os, byte[] bytes) throws IOException {
        os.write(bytes);
    }

    private void writeLine(OutputStream os, Charset defaultCharset) throws IOException {
        os.write(LINE_SEPARATOR.getBytes(defaultCharset));
    }

    private void writeEntity(OutputStream os, Object entity, MediaType mediaType, ResteasyReactiveRequestContext context)
            throws IOException {
        ServerSerialisers serializers = context.getDeployment().getSerialisers();
        Class<?> entityClass = entity.getClass();
        Type entityType = null;
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serializers
                .findWriters(null, entityClass, mediaType, RuntimeType.SERVER)
                .toArray(ServerSerialisers.NO_WRITER);
        boolean wrote = false;
        for (MessageBodyWriter<Object> writer : writers) {
            if (writer.isWriteable(entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType)) {
                // FIXME: spec doesn't really say what headers we should use here
                writer.writeTo(entity, entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType,
                        Serialisers.EMPTY_MULTI_MAP, os);
                wrote = true;
                break;
            }
        }

        if (!wrote) {
            throw new IllegalStateException("Could not find MessageBodyWriter for " + entityClass + " as " + mediaType);
        }
    }

    private String generateBoundary() {
        return UUID.randomUUID().toString();
    }

    private void appendBoundaryIntoMediaType(ResteasyReactiveRequestContext requestContext, String boundary,
            MediaType mediaType) {
        requestContext.setResponseContentType(new MediaType(mediaType.getType(), mediaType.getSubtype(),
                Collections.singletonMap(BOUNDARY_PARAM, boundary)));
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private boolean isListOf(PartItem part, Class<?> paramType) {
        if (!(part.getValue() instanceof Collection)) {
            return false;
        }

        return paramType.getName().equals(part.getFirstParamType());
    }
}
