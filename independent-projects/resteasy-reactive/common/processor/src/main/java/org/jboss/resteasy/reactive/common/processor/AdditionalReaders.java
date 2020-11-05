package org.jboss.resteasy.reactive.common.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.ext.MessageBodyReader;

public class AdditionalReaders {
    private final List<Entry<?>> entries = new ArrayList<>();

    public <T> void add(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass) {
        add(readerClass, mediaType, entityClass, null);
    }

    public <T> void add(Class<? extends MessageBodyReader<T>> readerClass, String mediaType, Class<T> entityClass,
            RuntimeType constraint) {

        Entry<T> newEntry = new Entry<>(readerClass, mediaType, entityClass, constraint);

        // we first attempt to "merge" readers if we encounter the same reader needed for both client and server
        Entry<?> matchingEntryIgnoringConstraint = null;
        for (Entry<?> entry : entries) {
            if (entry.matchesIgnoringConstraint(newEntry)) {
                matchingEntryIgnoringConstraint = entry;
                break;
            }
        }
        if (matchingEntryIgnoringConstraint != null) {
            if (matchingEntryIgnoringConstraint.constraint != newEntry.constraint) {
                // in this case we have a MessageBodyReader that applies to both client and server so
                // we remove the existing entity and replace it with one that has no constraint
                entries.remove(matchingEntryIgnoringConstraint);
                entries.add(new Entry<>(readerClass, mediaType, entityClass, null));
            } else {
                // nothing to do since the entries match completely
            }
        } else {
            entries.add(newEntry);
        }
    }

    public List<Entry<?>> get() {
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

        public boolean matchesIgnoringConstraint(Entry<?> other) {
            return readerClass.equals(other.readerClass) && entityClass.equals(other.entityClass)
                    && mediaType.equals(other.mediaType);
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
