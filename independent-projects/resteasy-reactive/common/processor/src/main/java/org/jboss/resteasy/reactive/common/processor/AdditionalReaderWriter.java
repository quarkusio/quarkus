package org.jboss.resteasy.reactive.common.processor;

import java.util.Objects;
import javax.ws.rs.RuntimeType;

public interface AdditionalReaderWriter {

    default void add(Class<?> handlerClass, String mediaType, Class<?> entityClass) {
        add(handlerClass, mediaType, entityClass, null);
    }

    void add(Class<?> handlerClass, String mediaType, Class<?> entityClass, RuntimeType constraint);

    class Entry {
        private final Class<?> handlerClass;
        private final String mediaType;
        private final Class<?> entityClass;
        private final RuntimeType constraint;

        public Entry(Class<?> handlerClass, String mediaType, Class<?> entityClass) {
            this(handlerClass, mediaType, entityClass, null);
        }

        public Entry(Class<?> handlerClass, String mediaType, Class<?> entityClass,
                RuntimeType constraint) {
            this.handlerClass = Objects.requireNonNull(handlerClass);
            this.mediaType = Objects.requireNonNull(mediaType);
            this.entityClass = Objects.requireNonNull(entityClass);
            this.constraint = constraint;
        }

        public Class<?> getHandlerClass() {
            return handlerClass;
        }

        public String getMediaType() {
            return mediaType;
        }

        public Class<?> getEntityClass() {
            return entityClass;
        }

        public RuntimeType getConstraint() {
            return constraint;
        }

        public boolean matchesIgnoringConstraint(Entry other) {
            return handlerClass.equals(other.handlerClass) && entityClass.equals(other.entityClass)
                    && mediaType.equals(other.mediaType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Entry entry = (Entry) o;
            return handlerClass.equals(entry.handlerClass) &&
                    mediaType.equals(entry.mediaType) &&
                    entityClass.equals(entry.entityClass) &&
                    constraint == entry.constraint;
        }

        @Override
        public int hashCode() {
            return Objects.hash(handlerClass, mediaType, entityClass, constraint);
        }
    }
}
