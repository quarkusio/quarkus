package io.quarkus.mongodb;

import java.io.Closeable;

/**
 * A reactive Mongo client.
 * Instances can represent either a standalone MongoDB instance, a replica set, or a sharded cluster. Instance of this
 * class are responsible for maintaining an up-to-date state of the cluster, and possibly cache resources related to
 * this, including background threads for monitoring, and connection pools.
 * <p>
 * Instance of this class server as factories for {@code ReactiveMongoDatabase} instances.
 * </p>
 */
@Deprecated
public interface ReactiveMongoClient extends Closeable, io.quarkus.mongodb.axle.ReactiveMongoClient {

}
