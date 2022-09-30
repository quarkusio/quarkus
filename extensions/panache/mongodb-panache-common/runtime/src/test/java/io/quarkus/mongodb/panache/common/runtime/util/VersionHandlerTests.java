package io.quarkus.mongodb.panache.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.mongodb.panache.common.Version;

public class VersionHandlerTests {

    private static final String VERSION = "version";
    private static final BsonDocument bsonDocument = new BsonDocument("_id", new BsonString("abc"));
    private static final BsonDocument emptyBsonDocument = new BsonDocument();

    @DisplayName("build EntityVersion with public version field, and reset version value")
    @Test
    public void handleEntityWithPublicVersion() {
        //test entity with public version and versionValue=0

        EntityWithPublicVersion entityWithPublicVersion = new EntityWithPublicVersion("abc", 0L);
        EntityVersionInfo entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithPublicVersion,
                bsonDocument);
        assertEquals(new BsonString(entityWithPublicVersion.id), entityVersionInfo.entityId);
        assertEquals(VERSION, entityVersionInfo.versionFieldName);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertTrue(entityVersionInfo.hasVersionValue);
        assertEquals(0L, entityVersionInfo.versionValue);
        assertEquals(1L, entityWithPublicVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertEquals(0L, entityWithPublicVersion.version);

        //test entity with public version and versionValue=null
        entityWithPublicVersion = new EntityWithPublicVersion(null, null);
        entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithPublicVersion, emptyBsonDocument);
        assertNull(entityVersionInfo.versionValue);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertFalse(entityVersionInfo.hasVersionValue);
        assertNull(entityVersionInfo.versionValue);
        assertEquals(0L, entityWithPublicVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertNull(entityWithPublicVersion.version);
    }

    @DisplayName("build EntityVersion with protected version field, and reset version value")
    @Test
    public void handleEntityWithProtectedVersion() {
        //test entity with public version and versionValue=0
        EntityWithProtectedVersion entityWithProtectedVersion = new EntityWithProtectedVersion(null, 0L);
        EntityVersionInfo entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithProtectedVersion,
                bsonDocument);

        assertEquals(VERSION, entityVersionInfo.versionFieldName);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertTrue(entityVersionInfo.hasVersionValue);
        assertEquals(0L, entityVersionInfo.versionValue);
        assertEquals(1L, entityWithProtectedVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertEquals(0L, entityWithProtectedVersion.version);

        //test entity with public version and versionValue=null
        entityWithProtectedVersion = new EntityWithProtectedVersion(null, null);
        entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithProtectedVersion, bsonDocument);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertFalse(entityVersionInfo.hasVersionValue);
        assertNull(entityVersionInfo.versionValue);
        assertEquals(0L, entityWithProtectedVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertNull(entityWithProtectedVersion.version);
    }

    @DisplayName("build EntityVersion with private version field, and reset version value")
    @Test
    public void handleEntityWithPrivateVersion() {
        //test entity with public version and versionValue=0
        EntityWithPrivateVersion entityWithPrivateVersion = new EntityWithPrivateVersion(null, 2L);
        EntityVersionInfo entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithPrivateVersion,
                bsonDocument);

        assertEquals("myVersion", entityVersionInfo.versionFieldName);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertTrue(entityVersionInfo.hasVersionValue);
        assertEquals(2L, entityVersionInfo.versionValue);
        assertEquals(3L, entityWithPrivateVersion.myVersion);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertEquals(2L, entityWithPrivateVersion.myVersion);

        //test entity with public version and versionValue=null
        entityWithPrivateVersion = new EntityWithPrivateVersion(null, null);
        entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithPrivateVersion, bsonDocument);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertFalse(entityVersionInfo.hasVersionValue);
        assertNull(entityVersionInfo.versionValue);
        assertEquals(0L, entityWithPrivateVersion.myVersion);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertNull(entityWithPrivateVersion.myVersion);
    }

    @DisplayName("build EntityVersion with inherit public version field, and reset version value")
    @Test
    public void handleEntityWithInheritVersion() {
        //test entity with public version and versionValue=0
        EntityWithInheritVersion entityWithInheritVersion = new EntityWithInheritVersion(0L, null);
        EntityVersionInfo entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithInheritVersion,
                bsonDocument);

        assertEquals(VERSION, entityVersionInfo.versionFieldName);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertTrue(entityVersionInfo.hasVersionValue);
        assertEquals(0L, entityVersionInfo.versionValue);
        assertEquals(1L, entityWithInheritVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertEquals(0L, entityWithInheritVersion.version);

        //test entity with public version and versionValue=null
        entityWithInheritVersion = new EntityWithInheritVersion(null, null);
        entityVersionInfo = VersionHandler.buildAndAdjustEntityVersionInfo(entityWithInheritVersion, bsonDocument);
        assertTrue(entityVersionInfo.hasVersionAnnotation);
        assertFalse(entityVersionInfo.hasVersionValue);
        assertNull(entityVersionInfo.versionValue);
        assertEquals(0L, entityWithInheritVersion.version);

        VersionHandler.rollbackVersion(entityVersionInfo);
        assertNull(entityWithInheritVersion.version);
    }

    @DisplayName("fail building EntityVersion with duplicated version fields")
    @Test
    public void handleEntityWithDuplicatedVersion() {
        EntityWithInheritDuplicatedVersion entity = new EntityWithInheritDuplicatedVersion(null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> VersionHandler.buildAndAdjustEntityVersionInfo(entity, bsonDocument));
    }

    @DisplayName("fail building EntityVersion with duplicated version fields in a deep inheritance")
    @Test
    public void handleEntityWithDeepInheritanceDuplicatedVersion() {
        EntityWithDeepInheritance entityWithDeepInheritance = new EntityWithDeepInheritance(null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> VersionHandler.buildAndAdjustEntityVersionInfo(entityWithDeepInheritance, bsonDocument));
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

    public static class EntityWithInheritDuplicatedVersion extends BaseWithPublicVersion {

        private String id;

        @Version
        public Long childVersion;

        public EntityWithInheritDuplicatedVersion(Long version, String id, Long childVersion) {
            super(version);
            this.id = id;
            this.childVersion = childVersion;
        }
    }

    public static class EntityWithDeepInheritance extends BaseWithPublicVersionAndInheritance {

        @Version
        private Long versionnn;

        public EntityWithDeepInheritance(Long version, Long versiona, Long versionnn) {
            super(version, versiona);
            this.versionnn = versionnn;
        }
    }

    public static class BaseWithPublicVersionAndInheritance extends BaseWithPublicVersion {

        @Version
        private Long versiona;

        public BaseWithPublicVersionAndInheritance(Long version, Long versiona) {
            super(version);
            this.versiona = versiona;
        }
    }

}
