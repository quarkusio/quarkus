package io.quarkus.rest.common.deployment.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.ext.MessageBodyWriter;

public class AdditionalWriters {
    private final List<Entry<?>> entries = new ArrayList<>();

    public <T> void add(Class<? extends MessageBodyWriter<T>> writerClass, String mediaType, Class<T> entityClass) {
        add(writerClass, mediaType, entityClass, null);
    }

    public <T> void add(Class<? extends MessageBodyWriter<T>> writerClass, String mediaType, Class<T> entityClass,
            RuntimeType constraint) {

        Entry<T> newEntry = new Entry<>(writerClass, mediaType, entityClass, constraint);

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
                entries.add(new Entry<>(writerClass, mediaType, entityClass, null));
            } else {
                // nothing to do since the entries match completely
            }
        } else {
            entries.add(newEntry);
        }

        entries.add(new Entry<>(writerClass, mediaType, entityClass, constraint));
    }

    public List<Entry<?>> get() {
        return entries;
    }

    public static class Entry<T> {
        private final Class<? extends MessageBodyWriter<T>> writerClass;
        private final String mediaType;
        private final Class<T> entityClass;
        private final RuntimeType constraint;

        public Entry(Class<? extends MessageBodyWriter<T>> writerClass, String mediaType, Class<T> entityClass,
                RuntimeType constraint) {
            this.writerClass = Objects.requireNonNull(writerClass);
            this.mediaType = Objects.requireNonNull(mediaType);
            this.entityClass = Objects.requireNonNull(entityClass);
            this.constraint = constraint;
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

        public RuntimeType getConstraint() {
            return constraint;
        }

        public boolean matchesIgnoringConstraint(Entry<?> other) {
            return writerClass.equals(other.writerClass) && entityClass.equals(other.entityClass)
                    && mediaType.equals(other.mediaType);
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
                    entityClass.equals(entry.entityClass) &&
                    constraint == entry.constraint;
        }

        @Override
        public int hashCode() {
            return Objects.hash(writerClass, mediaType, entityClass, constraint);
        }
    }
}
