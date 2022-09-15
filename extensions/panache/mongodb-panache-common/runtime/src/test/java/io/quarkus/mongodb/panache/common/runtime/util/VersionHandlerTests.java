package io.quarkus.mongodb.panache.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.mongodb.panache.common.Version;

public class VersionHandlerTests {

    @Test
    public void extractVersion() {
        assertTrue(VersionHandler.of(new EntityWithPublicVersion(null, 0L)).containsVersionValue());

        EntityWithPublicVersion entityWithPublicVersionNullValues = new EntityWithPublicVersion(null, null);
        assertTrue(VersionHandler.of(entityWithPublicVersionNullValues).containsVersionAnnotation());
        assertFalse(VersionHandler.of(entityWithPublicVersionNullValues).containsVersionValue());

        assertTrue(VersionHandler.of(new EntityWithProtectedVersion(null, 1L)).containsVersionValue());
        EntityWithProtectedVersion entityWithProtectedVersionNullValues = new EntityWithProtectedVersion(null, null);
        assertTrue(VersionHandler.of(entityWithProtectedVersionNullValues).containsVersionAnnotation());
        assertFalse(VersionHandler.of(entityWithProtectedVersionNullValues).containsVersionValue());

        assertTrue(VersionHandler.of(new EntityWithPrivateVersion(null, 1L)).containsVersionValue());
        EntityWithPrivateVersion entityWithPrivateVersionNullValues = new EntityWithPrivateVersion(null, null);
        assertTrue(VersionHandler.of(entityWithPrivateVersionNullValues).containsVersionAnnotation());
        assertFalse(VersionHandler.of(entityWithPrivateVersionNullValues).containsVersionValue());

        assertTrue(VersionHandler.of(new EntityWithInheritVersion(2L, null)).containsVersionValue());
        EntityWithInheritVersion entityWithInheritVersionNullValues = new EntityWithInheritVersion(null, null);
        assertTrue(VersionHandler.of(entityWithInheritVersionNullValues).containsVersionAnnotation());
        assertFalse(VersionHandler.of(entityWithInheritVersionNullValues).containsVersionValue());
    }

    public static class EntityWithPrivateVersion {

        private String id;

        @Version
        private Long version;

        public EntityWithPrivateVersion(String id, Long version) {
            this.id = id;
            this.version = version;
        }
    }

    public static class EntityWithPublicVersion {

        private String id;

        @Version
        public Long version;

        public EntityWithPublicVersion(String id, Long version) {
            this.id = id;
            this.version = version;
        }
    }

    public static class EntityWithProtectedVersion {

        private String id;

        @Version
        protected Long version;

        public EntityWithProtectedVersion(String id, Long version) {
            this.id = id;
            this.version = version;
        }
    }

    public static class EntityWithInheritVersion extends BaseWithPublicVersion {

        private String id;

        public EntityWithInheritVersion(Long version, String id) {
            super(version);
            this.id = id;
        }
    }

    public static class BaseWithPublicVersion {

        @Version
        public Long version;

        public BaseWithPublicVersion(Long version) {
            this.version = version;
        }
    }
}
