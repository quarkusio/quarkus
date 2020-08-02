package io.quarkus.funqy.gcp.functions.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Background function event for Storage
 *
 * @see <a href="https://cloud.google.com/storage/docs/json_api/v1/objects#resource">Storage resource object</a>
 */
public class StorageEvent {
    public String id;
    public String selfLink;
    public String name;
    public String bucket;
    public long generation;
    public long metageneration;
    public String contentType;
    public LocalDateTime timeCreated;
    public LocalDateTime updated;
    public LocalDateTime timeDeleted;
    public boolean temporaryHold;
    public boolean eventBasedHold;
    public LocalDateTime retentionExpirationTime;
    public String storageClass;
    public LocalDateTime timeStorageClassUpdated;
    public long size;
    public String md5Hash;
    public String mediaLink;
    public String contentEncoding;
    public String contentDisposition;
    public String contentLanguage;
    public String cacheControl;
    public Map metadata;
    public Owner owner;
    public String crc32c;
    public int componentCount;
    public String etag;
    public CustomerEncryption customerEncryption;
    public String kmsKeyName;

    public static class Owner {
        public String entity;
        public String entityId;
    }

    public static class CustomerEncryption {
        public String encryptionAlgorithm;
        public String keySha256;
    }
}
