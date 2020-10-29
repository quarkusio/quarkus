package io.quarkus.jaxrs.client.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;

import io.quarkus.rest.common.runtime.core.Serialisers;
import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;

public class QuarkusRestInboundSseEvent implements InboundSseEvent {

    private String id;
    private String name;
    private String comment;
    private String data;
    private MediaType mediaType;
    private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;
    private Serialisers serialisers;
    private QuarkusRestConfiguration configuration;

    public QuarkusRestInboundSseEvent(QuarkusRestConfiguration configuration, Serialisers serialisers) {
        this.configuration = configuration;
        this.serialisers = serialisers;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public QuarkusRestInboundSseEvent setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    public QuarkusRestInboundSseEvent setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public QuarkusRestInboundSseEvent setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public QuarkusRestInboundSseEvent setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay != -1;
    }

    public QuarkusRestInboundSseEvent setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
        return this;
    }

    @Override
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    @Override
    public String readData() {
        return data;
    }

    public QuarkusRestInboundSseEvent setData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public <T> T readData(Class<T> type) {
        return readData(type, mediaType);
    }

    @Override
    public <T> T readData(GenericType<T> type) {
        return readData(type, mediaType);
    }

    @Override
    public <T> T readData(Class<T> messageType, MediaType mediaType) {
        return readData(new GenericType<T>(messageType), mediaType);
    }

    @Override
    public <T> T readData(GenericType<T> type, MediaType mediaType) {
        InputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        try {
            return (T) ClientSerialisers.invokeClientReader(null, type.getRawType(), type.getType(),
                    mediaType, null, Serialisers.EMPTY_MULTI_MAP,
                    serialisers, in, Serialisers.NO_READER_INTERCEPTOR, configuration);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return "InboundSseEvent[data: " + data
                + ", name: " + name
                + ", id: " + id
                + ", comment: " + comment
                + ", mediaType: " + mediaType
                + ", reconnectDelay: " + reconnectDelay
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(mediaType, comment, data, id, name);
        result = prime * result + (int) (reconnectDelay ^ (reconnectDelay >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof QuarkusRestInboundSseEvent == false)
            return false;
        QuarkusRestInboundSseEvent other = (QuarkusRestInboundSseEvent) obj;
        return Objects.equals(getComment(), other.getComment())
                && Objects.equals(getMediaType(), other.getMediaType())
                && Objects.equals(getId(), other.getId())
                && Objects.equals(getName(), other.getName())
                && getReconnectDelay() == other.getReconnectDelay()
                && Objects.equals(readData(), other.readData());
    }
}
