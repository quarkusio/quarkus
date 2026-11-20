package io.quarkus.redis.lettuce.runtime.internal;

import io.smallrye.mutiny.Uni;

/**
 * Where the commands of a data source find the connection they run on: a {@link LettuceConnection} itself, for a
 * pooled or pinned connection, or the {@link LettuceSharedConnection} of a client, which its first command opens.
 */
public interface LettuceConnectionSource {

    /**
     * The connection, opened first when this is a shared connection nothing used yet. The {@code Uni} emits on the
     * thread that established the connection, or on the caller's when it is established already.
     */
    Uni<LettuceConnection> connect();

    /**
     * The connection when it is established and every command that waited for it was sent, {@code null} otherwise.
     * A command finding it here is sent at once, after the ones that waited.
     */
    LettuceConnection established();

    /**
     * Whether the connection is, or will be, to a cluster, where transactions and database selection are not
     * available. Known without connecting.
     */
    boolean isCluster();

}
