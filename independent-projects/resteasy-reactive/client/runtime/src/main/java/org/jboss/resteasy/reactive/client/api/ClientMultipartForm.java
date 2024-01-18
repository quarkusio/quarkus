package org.jboss.resteasy.reactive.client.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormDataPart;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

/**
 * This class allows programmatic creation of multipart requests
 */
public abstract class ClientMultipartForm {

    protected Charset charset = StandardCharsets.UTF_8;
    protected final List<QuarkusMultipartFormDataPart> parts = new ArrayList<>();
    protected final List<QuarkusMultipartForm.PojoFieldData> pojos = new ArrayList<>();

    public static ClientMultipartForm create() {
        return new QuarkusMultipartForm();
    }

    public ClientMultipartForm setCharset(String charset) {
        return setCharset(charset != null ? Charset.forName(charset) : null);
    }

    public ClientMultipartForm setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public ClientMultipartForm attribute(String name, String value, String filename) {
        parts.add(new QuarkusMultipartFormDataPart(name, value, filename));
        return this;
    }

    public ClientMultipartForm entity(String name, Object entity, String mediaType, Class<?> type) {
        return entity(name, null, entity, mediaType, type);
    }

    public ClientMultipartForm entity(String name, String filename, Object entity, String mediaType, Class<?> type) {
        pojos.add(new QuarkusMultipartForm.PojoFieldData(name, filename, entity, mediaType, type, parts.size()));
        parts.add(null); // make place for ^
        return this;
    }

    public ClientMultipartForm textFileUpload(String name, String filename, String pathname, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, pathname, mediaType, true));
        return this;
    }

    public ClientMultipartForm textFileUpload(String name, String filename, Buffer content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, true));
        return this;
    }

    public ClientMultipartForm stringFileUpload(String name, String filename, String content, String mediaType) {
        return textFileUpload(name, filename, Buffer.buffer(content), mediaType);
    }

    public ClientMultipartForm binaryFileUpload(String name, String filename, String pathname, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, pathname, mediaType, false));
        return this;
    }

    public ClientMultipartForm binaryFileUpload(String name, String filename, Buffer content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, false));
        return this;
    }

    public ClientMultipartForm multiAsBinaryFileUpload(String name, String filename, Multi<Byte> content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, false));
        return this;
    }

    public ClientMultipartForm multiAsTextFileUpload(String name, String filename, Multi<Byte> content, String mediaType) {
        parts.add(new QuarkusMultipartFormDataPart(name, filename, content, mediaType, true));
        return this;
    }

}
