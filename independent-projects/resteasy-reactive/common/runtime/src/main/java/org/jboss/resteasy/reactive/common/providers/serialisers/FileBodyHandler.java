package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.common.headers.HeaderUtil;

public class FileBodyHandler implements MessageBodyReader<File>, MessageBodyWriter<File> {
    public static final String PREFIX = "pfx";
    public static final String SUFFIX = "sfx";

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class == type;
    }

    @Override
    public File readFrom(Class<File> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return doRead(httpHeaders, entityStream, Files.createTempFile(PREFIX, SUFFIX).toFile());
    }

    public static File doRead(MultivaluedMap<String, String> httpHeaders, InputStream entityStream,
            File file) throws IOException {
        if (HeaderUtil.isContentLengthZero(httpHeaders)) {
            return file;
        }

        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
            entityStream.transferTo(output);
        }

        return file;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    public void writeTo(File uploadFile, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(uploadFile.length()));
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
}
