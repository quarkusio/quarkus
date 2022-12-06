package org.jboss.resteasy.reactive.client.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEvent;

import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

public class InboundSseEventImpl implements InboundSseEvent {

    private String id;
    private String name;
    private String comment;
    private String data;
    private MediaType mediaType;
    private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;
    private Serialisers serialisers;
    private ConfigurationImpl configuration;

    public InboundSseEventImpl(ConfigurationImpl configuration, Serialisers serialisers) {
        this.configuration = configuration;
        this.serialisers = serialisers;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public InboundSseEventImpl setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    public InboundSseEventImpl setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public InboundSseEventImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public InboundSseEventImpl setComment(String comment) {
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

    public InboundSseEventImpl setReconnectDelay(long reconnectDelay) {
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

    public InboundSseEventImpl setData(String data) {
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
                    mediaType, null, null, Serialisers.EMPTY_MULTI_MAP,
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
        if (obj instanceof InboundSseEventImpl == false)
            return false;
        InboundSseEventImpl other = (InboundSseEventImpl) obj;
        return Objects.equals(getComment(), other.getComment())
                && Objects.equals(getMediaType(), other.getMediaType())
                && Objects.equals(getId(), other.getId())
                && Objects.equals(getName(), other.getName())
                && getReconnectDelay() == other.getReconnectDelay()
                && Objects.equals(readData(), other.readData());
    }
}
