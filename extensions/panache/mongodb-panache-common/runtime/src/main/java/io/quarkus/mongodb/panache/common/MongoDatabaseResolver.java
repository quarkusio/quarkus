package io.quarkus.mongodb.panache.common;

/*
 * This interface can be used to resolve the MongoDB database name at runtime, it allows to implement multi-tenancy using a tenant per database approach.
 */
public interface MongoDatabaseResolver {
    String resolve();
}
