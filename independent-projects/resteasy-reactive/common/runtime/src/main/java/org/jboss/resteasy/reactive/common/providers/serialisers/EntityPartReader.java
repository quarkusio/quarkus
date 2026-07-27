package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.jboss.resteasy.reactive.common.jaxrs.EntityPartImpl;
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
        return parseMultipart(body, boundary);
    }

    private List<EntityPart> parseMultipart(byte[] body, String boundary) throws IOException {
        List<EntityPart> parts = new ArrayList<>();
        byte[] delimiterBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        byte[] closeBytes = ("--" + boundary + "--").getBytes(StandardCharsets.US_ASCII);
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);

        int pos = indexOf(body, delimiterBytes, 0);
        if (pos < 0) {
            return parts;
        }
        pos += delimiterBytes.length;
        if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') {
            pos += 2;
        }

        while (pos < body.length) {
            int nextDelimiter = indexOf(body, delimiterBytes, pos);
            if (nextDelimiter < 0) {
                break;
            }

            int partEnd = nextDelimiter;
            if (partEnd >= 2 && body[partEnd - 2] == '\r' && body[partEnd - 1] == '\n') {
                partEnd -= 2;
            }

            EntityPart part = parsePart(body, pos, partEnd);
            if (part != null) {
                parts.add(part);
            }

            pos = nextDelimiter + delimiterBytes.length;
            if (pos + 1 < body.length && body[pos] == '-' && body[pos + 1] == '-') {
                break;
            }
            if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') {
                pos += 2;
            }
        }

        return parts;
    }

    private EntityPart parsePart(byte[] body, int start, int end) {
        int headerEnd = indexOfDoubleCrlf(body, start, end);
        if (headerEnd < 0) {
            return null;
        }

        String headerSection = new String(body, start, headerEnd - start, StandardCharsets.UTF_8);
        int contentStart = headerEnd + 4;

        MultivaluedMap<String, String> headers = new QuarkusMultivaluedHashMap<>();
        String[] headerLines = headerSection.split("\r\n");
        for (String line : headerLines) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String headerName = line.substring(0, colon).trim();
                String headerValue = line.substring(colon + 1).trim();
                headers.add(headerName, headerValue);
            }
        }

        String name = null;
        String fileName = null;
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition != null) {
            name = extractParam(contentDisposition, "name");
            fileName = extractParam(contentDisposition, "filename");
        }
        if (name == null) {
            return null;
        }

        MediaType partMediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        String contentType = headers.getFirst("Content-Type");
        if (contentType != null) {
            partMediaType = MediaType.valueOf(contentType);
        }

        byte[] contentBytes = new byte[end - contentStart];
        System.arraycopy(body, contentStart, contentBytes, 0, contentBytes.length);

        return new EntityPartImpl(name, fileName, headers, partMediaType, new ByteArrayInputStream(contentBytes));
    }

    private String extractParam(String header, String paramName) {
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

    private int indexOfDoubleCrlf(byte[] data, int from, int to) {
        for (int i = from; i <= to - 4; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(byte[] data, byte[] pattern, int from) {
        outer: for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
