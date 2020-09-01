package io.quarkus.rest.runtime.jaxrs;

import java.lang.reflect.Type;
import java.util.Objects;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;

public class QuarkusRestOutboundSseEvent implements OutboundSseEvent {
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

    QuarkusRestOutboundSseEvent(final String name, final String id, final long reconnectDelay, final Class<?> type,
            final Type genericType,
            final MediaType mediaType, final Object data, final String comment) {
        this.name = name;
        this.comment = comment;
        this.id = id;
        this.reconnectDelay = reconnectDelay;
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public boolean isReconnectDelaySet() {
        return reconnectDelay > -1;
    }

    public Class<?> getType() {
        return type;
    }

    public Type getGenericType() {
        return genericType;
    }

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

    public String getComment() {
        return comment;
    }

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

        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder reconnectDelay(long milliseconds) {
            if (milliseconds < 0) {
                milliseconds = SseEvent.RECONNECT_NOT_SET;
            }
            this.reconnectDelay = milliseconds;
            return this;
        }

        public Builder mediaType(final MediaType mediaType) {
            Objects.requireNonNull(mediaType);
            this.mediaType = mediaType;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder data(Class type, Object data) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(data);

            this.type = type;
            this.genericType = type;
            this.data = data;
            return this;
        }

        public Builder data(GenericType type, Object data) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(data);

            this.type = type.getRawType();
            this.genericType = type.getType();
            this.data = data;
            return this;
        }

        public Builder data(Object data) {
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

        public OutboundSseEvent build() {
            if (this.comment == null && this.data == null) {
                throw new IllegalArgumentException("Must set either comment or data");
            }
            return new QuarkusRestOutboundSseEvent(name, id, reconnectDelay, type, genericType, mediaType, data, comment);
        }
    }
}
