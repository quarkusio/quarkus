package io.quarkus.rest.client.reactive.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;

/**
 * Simplified multipart form data parser for tests.
 * Based on RESTEasy's MultipartFormDataInputImpl/MultipartInputImpl
 * but stripped of all integration code (providers, workers, etc.).
 */
public class SimpleMultipartInput implements AutoCloseable {

    private final MediaType contentType;
    private Message mimeMessage;
    private final List<Part> parts = new ArrayList<>();
    private Map<String, List<Part>> formDataMap;

    public SimpleMultipartInput(MediaType contentType) {
        this.contentType = contentType;
    }

    public void parse(InputStream is) throws IOException {
        DefaultMessageBuilder builder = new DefaultMessageBuilder();
        mimeMessage = builder.parseMessage(addHeaderToHeadlessStream(is));
        extractParts();
    }

    private InputStream addHeaderToHeadlessStream(InputStream is) {
        String header = HttpHeaders.CONTENT_TYPE + ": " + contentType + "\r\n\r\n";
        return new SequenceInputStream(
                new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8)), is);
    }

    private void extractParts() throws IOException {
        Multipart multipart = (Multipart) mimeMessage.getBody();
        for (Entity entity : multipart.getBodyParts()) {
            if (entity instanceof BodyPart) {
                BodyPart bodyPart = (BodyPart) entity;
                Part part = new Part(bodyPart);

                Field disposition = bodyPart.getHeader().getField(FieldName.CONTENT_DISPOSITION);
                if (disposition instanceof ContentDispositionField) {
                    String name = ((ContentDispositionField) disposition).getParameter("name");
                    getFormDataMap().computeIfAbsent(name, k -> new LinkedList<>()).add(part);
                }

                parts.add(part);
            }
        }
    }

    public List<Part> getParts() {
        return parts;
    }

    public Map<String, List<Part>> getFormDataMap() {
        if (formDataMap == null) {
            formDataMap = new LinkedHashMap<>();
        }
        return formDataMap;
    }

    public String getPreamble() {
        return ((Multipart) mimeMessage.getBody()).getPreamble();
    }

    @Override
    public void close() {
        if (mimeMessage != null) {
            mimeMessage.dispose();
        }
    }

    public static class Part {

        private static final MediaType DEFAULT_CONTENT_TYPE = new MediaType("text", "plain",
                Map.of("charset", "us-ascii"));

        private final BodyPart bodyPart;
        private final MediaType contentType;
        private final MultivaluedMap<String, String> headers;

        Part(BodyPart bodyPart) {
            this.bodyPart = bodyPart;
            this.headers = new MultivaluedHashMap<>();
            MediaType ct = null;
            for (Field field : bodyPart.getHeader()) {
                headers.add(field.getName(), field.getBody());
                if (field instanceof ContentTypeField) {
                    ct = MediaType.valueOf(field.getBody());
                }
            }
            this.contentType = ct != null ? ct : DEFAULT_CONTENT_TYPE;
        }

        public InputStream getBody() throws IOException {
            if (bodyPart.getBody() instanceof SingleBody) {
                return ((SingleBody) bodyPart.getBody()).getInputStream();
            }
            return null;
        }

        public String getBodyAsString() throws IOException {
            try (InputStream is = getBody()) {
                return is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : null;
            }
        }

        public String getFileName() {
            return bodyPart.getFilename();
        }

        public MultivaluedMap<String, String> getHeaders() {
            return headers;
        }

        public MediaType getMediaType() {
            return contentType;
        }
    }
}
