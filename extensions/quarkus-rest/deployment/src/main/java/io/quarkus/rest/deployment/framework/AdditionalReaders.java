package io.quarkus.rest.deployment.framework;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.ext.MessageBodyReader;

public class AdditionalReaders {
    private final Set<Entry<?>> entries = new HashSet<>();

    public <T> void add(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass) {
        entries.add(new Entry<>(readerClass, mediaType, entityClass));
    }

    public <T> void add(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass,
            RuntimeType constraint) {
        entries.add(new Entry<>(readerClass, mediaType, entityClass, constraint));
    }

    public Set<Entry<?>> get() {
        return entries;
    }

    public static class Entry<T> {
        private final Class<? extends MessageBodyReader<T>> readerClass;
        private final String mediaType;
        private final Class<T> entityClass;
        private final RuntimeType constraint;

        public Entry(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass) {
            this(readerClass, mediaType, entityClass, null);
        }

        public Entry(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass,
                RuntimeType constraint) {
            this.readerClass = Objects.requireNonNull(readerClass);
            this.mediaType = Objects.requireNonNull(mediaType);
            this.entityClass = Objects.requireNonNull(entityClass);
            this.constraint = constraint;
        }

        public Class<? extends MessageBodyReader<T>> getReaderClass() {
            return readerClass;
        }

        public String getMediaType() {
            return mediaType;
        }

        public Class<T> getEntityClass() {
            return entityClass;
        }

        public RuntimeType getConstraint() {
            return constraint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Entry<?> entry = (Entry<?>) o;
            return readerClass.equals(entry.readerClass) &&
                    mediaType.equals(entry.mediaType) &&
                    entityClass.equals(entry.entityClass) &&
                    constraint == entry.constraint;
        }

        @Override
        public int hashCode() {
            return Objects.hash(readerClass, mediaType, entityClass, constraint);
        }
    }
}
