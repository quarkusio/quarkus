package org.jboss.resteasy.reactive.common.processor;

import jakarta.ws.rs.RuntimeType;
import java.util.Objects;

public interface AdditionalReaderWriter {

    default void add(String handlerClass, String mediaType, String entityClass) {
        add(handlerClass, mediaType, entityClass, null);
    }

    default void add(Class<?> handlerClass, String mediaType, Class<?> entityClass) {
        add(handlerClass.getName(), mediaType, entityClass.getName(), null);
    }

    default void add(Class<?> handlerClass, String mediaType, Class<?> entityClass, RuntimeType constraint) {
        add(handlerClass.getName(), mediaType, entityClass.getName(), constraint);
    }

    void add(String handlerClass, String mediaType, String entityClass, RuntimeType constraint);

    class Entry {
        private final String handlerClass;
        private final String mediaType;
        private final String entityClass;
        private final RuntimeType constraint;

        public Entry(String handlerClass, String mediaType, String entityClass) {
            this(handlerClass, mediaType, entityClass, null);
        }

        public Entry(String handlerClass, String mediaType, String entityClass,
                RuntimeType constraint) {
            this.handlerClass = Objects.requireNonNull(handlerClass);
            this.mediaType = Objects.requireNonNull(mediaType);
            this.entityClass = Objects.requireNonNull(entityClass);
            this.constraint = constraint;
        }

        public String getHandlerClass() {
            return handlerClass;
        }

        public String getMediaType() {
            return mediaType;
        }

        public String getEntityClass() {
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
