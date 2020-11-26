package org.jboss.resteasy.reactive.server.jaxrs;

import java.lang.reflect.Type;
import java.util.Objects;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;

public class OutboundSseEventImpl implements OutboundSseEvent {
    private final String name;

    private final String comment;

    private final String id;

    private final Class<?> type;

    private final Type genericType;

    private MediaType mediaType;

    private boolean mediaTypeSet;

    private final Object data;

    private final long reconnectDelay;

    private boolean escape = false;

    OutboundSseEventImpl(final String name, final String id, final long reconnectDelay, final Class<?> type,
            final Type genericType,
            final MediaType mediaType, final Object data, final String comment) {
        this.name = name;
        this.comment = comment;
        this.id = id;
        this.reconnectDelay = reconnectDelay;
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType != null ? mediaType : MediaType.TEXT_PLAIN_TYPE;
        this.mediaTypeSet = mediaType != null;
        this.data = data;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay > -1;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isMediaTypeSet() {
        return mediaTypeSet;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        mediaTypeSet = true;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public Object getData() {
        return data;
    }

    public boolean isEscape() {
        return escape;
    }

    public void setEscape(Boolean escape) {
        this.escape = escape;
    }

    public static class BuilderImpl implements Builder {
        private String name;

        private String comment;

        private String id;

        private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;

        private Class<?> type;

        private Type genericType;

        private Object data;

        private MediaType mediaType;

        @Override
        public BuilderImpl name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public BuilderImpl id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public BuilderImpl reconnectDelay(long milliseconds) {
            if (milliseconds < 0) {
                milliseconds = SseEvent.RECONNECT_NOT_SET;
            }
            this.reconnectDelay = milliseconds;
            return this;
        }

        @Override
        public BuilderImpl mediaType(final MediaType mediaType) {
            Objects.requireNonNull(mediaType);
            this.mediaType = mediaType;
            return this;
        }

        @Override
        public BuilderImpl comment(String comment) {
            this.comment = comment;
            return this;
        }

        @Override
        public BuilderImpl data(Class type, Object data) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(data);

            this.type = type;
            this.genericType = type;
            this.data = data;
            return this;
        }

        @Override
        public BuilderImpl data(GenericType type, Object data) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(data);

            this.type = type.getRawType();
            this.genericType = type.getType();
            this.data = data;
            return this;
        }

        @Override
        public BuilderImpl data(Object data) {
            Objects.requireNonNull(data);

            if (data instanceof GenericEntity) {
                GenericEntity<?> genericEntity = (GenericEntity<?>) data;
                this.type = genericEntity.getRawType();
                this.genericType = genericEntity.getType();
                this.data = genericEntity.getEntity();
            } else {
                data(data.getClass(), data);
            }

            return this;
        }

        @Override
        public OutboundSseEventImpl build() {
            if (this.comment == null && this.data == null) {
                throw new IllegalArgumentException("Must set either comment or data");
            }
            return new OutboundSseEventImpl(name, id, reconnectDelay, type, genericType, mediaType, data, comment);
        }
    }
}
