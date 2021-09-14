package org.jboss.resteasy.reactive.client.impl.multipart;

import io.vertx.core.buffer.Buffer;

/**
 * based on {@link io.vertx.ext.web.multipart.impl.FormDataPartImpl}
 */
public class QuarkusMultipartFormDataPart {

    private final String name;
    private final String value;
    private final String filename;
    private final String mediaType;
    private final String pathname;
    private final boolean text;
    private final boolean isObject;
    private final Class<?> type;
    private final Buffer content;

    public QuarkusMultipartFormDataPart(String name, Buffer content, String mediaType, Class<?> type) {
        this.name = name;
        this.content = content;
        this.mediaType = mediaType;
        this.type = type;

        if (name == null) {
            throw new NullPointerException("Multipart field name cannot be null");
        }
        if (mediaType == null) {
            throw new NullPointerException("Multipart field media type cannot be null");
        }
        if (type == null) {
            throw new NullPointerException("Multipart field media type cannot be null");
        }
        this.isObject = true;
        this.value = null;
        this.filename = null;
        this.pathname = null;
        this.text = false;
    }

    public QuarkusMultipartFormDataPart(String name, String value) {
        if (name == null) {
            throw new NullPointerException("Multipart field name cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("Multipart field value cannot be null");
        }
        this.name = name;
        this.value = value;
        this.filename = null;
        this.pathname = null;
        this.content = null;
        this.mediaType = null;
        this.text = false;
        this.isObject = false;
        this.type = null;
    }

    public QuarkusMultipartFormDataPart(String name, String filename, String pathname, String mediaType, boolean text) {
        if (name == null) {
            throw new NullPointerException("Multipart field name cannot be null");
        }
        if (filename == null) {
            throw new NullPointerException("Multipart field name filename cannot be null when sending files");
        }
        if (pathname == null) {
            throw new NullPointerException("Multipart field name pathname cannot be null when sending files");
        }
        if (mediaType == null) {
            throw new NullPointerException("Multipart field media type cannot be null");
        }
        this.name = name;
        this.value = null;
        this.filename = filename;
        this.pathname = pathname;
        this.content = null;
        this.mediaType = mediaType;
        this.text = text;
        this.isObject = false;
        this.type = null;
    }

    public QuarkusMultipartFormDataPart(String name, String filename, Buffer content, String mediaType, boolean text) {
        if (name == null) {
            throw new NullPointerException("Multipart field name cannot be null");
        }
        if (filename == null) {
            throw new NullPointerException("Multipart field name filename cannot be null when sending files");
        }
        if (content == null) {
            throw new NullPointerException("Multipart field name content cannot be null when sending files");
        }
        if (mediaType == null) {
            throw new NullPointerException("Multipart field media type cannot be null");
        }
        this.name = name;
        this.value = null;
        this.filename = filename;
        this.pathname = null;
        this.content = content;
        this.mediaType = mediaType;
        this.text = text;
        this.isObject = false;
        this.type = null;
    }

    public String name() {
        return name;
    }

    public boolean isAttribute() {
        return value != null;
    }

    public boolean isObject() {
        return isObject;
    }

    public String value() {
        return value;
    }

    public String filename() {
        return filename;
    }

    public String pathname() {
        return pathname;
    }

    public Buffer content() {
        return content;
    }

    public String mediaType() {
        return mediaType;
    }

    public boolean isText() {
        return text;
    }

    public Class<?> getType() {
        return type;
    }
}
