package io.quarkus.rest.deployment.framework;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyWriter;

public class AdditionalWriters {
    private final Set<Entry<?>> entries = new HashSet<>();

    public <T> void add(Class<? extends MessageBodyWriter<T>> readerClass, String mediaType, Class<T> entityClass) {
        entries.add(new Entry<>(readerClass, mediaType, entityClass));
    }

    public Set<Entry<?>> get() {
        return entries;
    }

    public static class Entry<T> {
        private final Class<? extends MessageBodyWriter<T>> writerClass;
        private final String mediaType;
        private final Class<T> entityClass;

        public Entry(Class<? extends MessageBodyWriter<T>> writerClass, String mediaType, Class<T> entityClass) {
            this.writerClass = Objects.requireNonNull(writerClass);
            this.mediaType = Objects.requireNonNull(mediaType);
            this.entityClass = Objects.requireNonNull(entityClass);
        }

        public Class<? extends MessageBodyWriter<T>> getWriterClass() {
            return writerClass;
        }

        public String getMediaType() {
            return mediaType;
        }

        public Class<T> getEntityClass() {
            return entityClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Entry<?> entry = (Entry<?>) o;
            return writerClass.equals(entry.writerClass) &&
                    mediaType.equals(entry.mediaType) &&
                    entityClass.equals(entry.entityClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(writerClass, mediaType, entityClass);
        }
    }
}
