package io.quarkus.rest.runtime.providers.serialisers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.headers.HeaderUtil;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

// TODO: this is very simplistic at the moment

@Provider
@Produces("*/*")
@Consumes("*/*")
public class FileBodyHandler implements MessageBodyReader<File>, QuarkusRestMessageBodyWriter<File> {
    private static final String PREFIX = "pfx";
    private static final String SUFFIX = "sfx";

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class == type;
    }

    @Override
    public File readFrom(Class<File> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        File downloadedFile = File.createTempFile(PREFIX, SUFFIX);
        if (HeaderUtil.isContentLengthZero(httpHeaders)) {
            return downloadedFile;
        }

        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(downloadedFile))) {
            int read;
            final byte[] buf = new byte[2048];
            while ((read = entityStream.read(buf)) != -1) {
                output.write(buf, 0, read);
            }
        }

        return downloadedFile;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public void writeTo(File uploadFile, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        doWrite(uploadFile, entityStream);
    }

    protected void doWrite(File uploadFile, OutputStream out) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(uploadFile))) {
            int read;
            final byte[] buf = new byte[2048];
            while ((read = inputStream.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, QuarkusRestRequestContext context) throws WebApplicationException {
        HttpServerResponse vertxResponse = context.getContext().response();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            doWrite(o, baos);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        vertxResponse.end(Buffer.buffer(baos.toByteArray()));
    }
}
