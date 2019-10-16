package io.quarkus.redis.runtime;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.redis.runtime.commands.QuarkusRedisClusterSyncCommands;

public class QuarkusRedisClusterConnection<K, V> implements StatefulRedisClusterConnection<K, V> {

    private StatefulRedisClusterConnection<K, V> clusterConnection;

    public QuarkusRedisClusterConnection(StatefulRedisClusterConnection<K, V> connection) {
        this.clusterConnection = connection;
    }

    @Override
    public RedisAdvancedClusterCommands<K, V> sync() {
        return new QuarkusRedisClusterSyncCommands<>(this, async());
    }

    @Override
    public RedisAdvancedClusterAsyncCommands<K, V> async() {
        return clusterConnection.async();
    }

    @Override
    public RedisAdvancedClusterReactiveCommands<K, V> reactive() {
        return clusterConnection.reactive();
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String s) {
        return clusterConnection.getConnection(s);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String s) {
        return clusterConnection.getConnectionAsync(s);
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String s, int i) {
        return clusterConnection.getConnection(s, i);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String s, int i) {
        return clusterConnection.getConnectionAsync(s, i);
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        clusterConnection.setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return clusterConnection.getReadFrom();
    }

    @Override
    public Partitions getPartitions() {
        return clusterConnection.getPartitions();
    }

    @Override
    public RedisChannelWriter getChannelWriter() {
        return clusterConnection.getChannelWriter();
    }

    @Override
    public void setTimeout(Duration duration) {
        clusterConnection.setTimeout(duration);
    }

    @Override
    @Deprecated
    public void setTimeout(long l, TimeUnit timeUnit) {
        clusterConnection.setTimeout(l, timeUnit);
    }

    @Override
    public Duration getTimeout() {
        return clusterConnection.getTimeout();
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> redisCommand) {
        return clusterConnection.dispatch(redisCommand);
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> collection) {
        return clusterConnection.dispatch(collection);
    }

    @Override
    public void close() {
        clusterConnection.close();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return clusterConnection.closeAsync();
    }

    @Override
    public boolean isOpen() {
        return clusterConnection.isOpen();
    }

    @Override
    public ClientOptions getOptions() {
        return clusterConnection.getOptions();
    }

    @Override
    public ClientResources getResources() {
        return clusterConnection.getResources();
    }

    @Override
    public void reset() {
        clusterConnection.reset();
    }

    @Override
    public void setAutoFlushCommands(boolean b) {
        clusterConnection.setAutoFlushCommands(b);
    }

    @Override
    public void flushCommands() {
        clusterConnection.flushCommands();
    }
}
