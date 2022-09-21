package io.quarkus.mongodb.panache.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;

import io.quarkus.mongodb.panache.common.Version;

public class VersionHandlerTests {

    private static final String VERSION = "version";

    @Test
    public void extractVersion() {

        BsonDocument bsonDocument = new BsonDocument("_id", new BsonString("abc"));

        assertTrue(VersionHandler.handle(new EntityWithPublicVersion(null, 0L), bsonDocument).containsVersionValue);

        EntityWithPublicVersion entityWithPublicVersionNullValues = new EntityWithPublicVersion(null, null);
        VersionHandler versionHandler = VersionHandler.handle(entityWithPublicVersionNullValues, bsonDocument);

        assertEquals(VERSION, versionHandler.versionFieldName);
        assertTrue(versionHandler.containsVersionAnnotation);
        assertFalse(versionHandler.containsVersionValue);

        assertTrue(VersionHandler.handle(new EntityWithProtectedVersion(null, 1L), bsonDocument).containsVersionValue);
        EntityWithProtectedVersion entityWithProtectedVersionNullValues = new EntityWithProtectedVersion(null, null);

        versionHandler = VersionHandler.handle(entityWithProtectedVersionNullValues, bsonDocument);
        assertEquals(VERSION, versionHandler.versionFieldName);
        assertTrue(versionHandler.containsVersionAnnotation);
        assertFalse(versionHandler.containsVersionValue);

        assertTrue(VersionHandler.handle(new EntityWithPrivateVersion(null, 1L), bsonDocument).containsVersionValue);
        EntityWithPrivateVersion entityWithPrivateVersionNullValues = new EntityWithPrivateVersion(null, null);
        versionHandler = VersionHandler.handle(entityWithPrivateVersionNullValues, bsonDocument);
        assertEquals("myVersion", versionHandler.versionFieldName);
        assertTrue(versionHandler.containsVersionAnnotation);
        assertFalse(versionHandler.containsVersionValue);

        assertTrue(VersionHandler.handle(new EntityWithInheritVersion(2L, null), bsonDocument).containsVersionValue);
        EntityWithInheritVersion entityWithInheritVersionNullValues = new EntityWithInheritVersion(null, null);
        versionHandler = VersionHandler.handle(entityWithInheritVersionNullValues, bsonDocument);
        assertEquals(VERSION, versionHandler.versionFieldName);
        assertTrue(versionHandler.containsVersionAnnotation);
        assertFalse(versionHandler.containsVersionValue);
    }

    public static class EntityWithPrivateVersion {

        private String id;

        @Version
        private Long myVersion;

        public EntityWithPrivateVersion(String id, Long myVersion) {
            this.id = id;
            this.myVersion = myVersion;
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
