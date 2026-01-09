package io.quarkus.infinispan.client.runtime.jfr;

import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.ExecutionMode.ASYNC;
import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.ExecutionMode.SYNC;
import static io.quarkus.infinispan.client.runtime.jfr.JfrCacheOperation.Scope.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.transaction.TransactionManager;

import org.infinispan.client.hotrod.*;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.infinispan.commons.api.query.ContinuousQuery;
import org.infinispan.commons.api.query.Query;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.commons.util.IntSet;
import org.reactivestreams.Publisher;

public class JfrRemoteCacheWrapper<K, V> implements RemoteCache<K, V> {

    public JfrRemoteCacheWrapper(RemoteCache<K, V> delegate) {
        this.delegate = delegate;
    }

    RemoteCache<K, V> delegate;

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean removeWithVersion(K key, long version) {
        return delegate.removeWithVersion(key, version);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V remove(Object key) {
        return delegate.remove(key);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        return delegate.replace(key, oldValue, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
            TimeUnit maxIdleTimeUnit) {
        return delegate.replace(key, oldValue, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> removeWithVersionAsync(K key, long version) {
        return delegate.removeWithVersionAsync(key, version);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replaceWithVersion(K key, V newValue, long version) {
        return delegate.replaceWithVersion(key, newValue, version);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, int lifespanSeconds) {
        return delegate.replaceWithVersion(key, newValue, version, lifespanSeconds);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, int lifespanSeconds, int maxIdleTimeSeconds) {
        return delegate.replaceWithVersion(key, newValue, version, lifespanSeconds, maxIdleTimeSeconds);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, long lifespan, TimeUnit lifespanTimeUnit, long maxIdle,
            TimeUnit maxIdleTimeUnit) {
        return delegate.replaceWithVersion(key, newValue, version, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version) {
        return delegate.replaceWithVersionAsync(key, newValue, version);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, int lifespanSeconds) {
        return delegate.replaceWithVersionAsync(key, newValue, version, lifespanSeconds);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, int lifespanSeconds,
            int maxIdleSeconds) {
        return delegate.replaceWithVersionAsync(key, newValue, version, lifespanSeconds, maxIdleSeconds);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, long lifespanSeconds,
            TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
        return delegate.replaceWithVersionAsync(key, newValue, version, lifespanSeconds, lifespanTimeUnit, maxIdle,
                maxIdleTimeUnit);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory, Set<Integer> segments,
            int batchSize) {
        return delegate.retrieveEntries(filterConverterFactory, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory,
            Object[] filterConverterParams, Set<Integer> segments, int batchSize) {
        return delegate.retrieveEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
    }

    @Override
    public <E> Publisher<Entry<K, E>> publishEntries(String filterConverterFactory, Object[] filterConverterParams,
            Set<Integer> segments, int batchSize) {
        return delegate.publishEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory, int batchSize) {
        return delegate.retrieveEntries(filterConverterFactory, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntriesByQuery(Query<?> filterQuery, Set<Integer> segments,
            int batchSize) {
        return delegate.retrieveEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public <E> Publisher<Entry<K, E>> publishEntriesByQuery(Query<?> filterQuery, Set<Integer> segments, int batchSize) {
        return delegate.publishEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, MetadataValue<Object>>> retrieveEntriesWithMetadata(Set<Integer> segments,
            int batchSize) {
        return delegate.retrieveEntriesWithMetadata(segments, batchSize);
    }

    @Override
    public Publisher<Entry<K, MetadataValue<V>>> publishEntriesWithMetadata(Set<Integer> segments, int batchSize) {
        return delegate.publishEntriesWithMetadata(segments, batchSize);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public MetadataValue<V> getWithMetadata(K key) {
        return delegate.getWithMetadata(key);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<MetadataValue<V>> getWithMetadataAsync(K key) {
        return delegate.getWithMetadataAsync(key);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorSet<K> keySet() {
        return delegate.keySet();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorSet<K> keySet(IntSet segments) {
        return delegate.keySet(segments);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorCollection<V> values() {
        return delegate.values();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorCollection<V> values(IntSet segments) {
        return delegate.values(segments);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet(IntSet segments) {
        return delegate.entrySet(segments);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = MULTI)
    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        delegate.putAll(map, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = MULTI)
    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime,
            TimeUnit maxIdleTimeUnit) {
        delegate.putAll(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = MULTI)
    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data) {
        return delegate.putAllAsync(data);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = MULTI)
    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit unit) {
        return delegate.putAllAsync(data, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = MULTI)
    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit,
            long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.putAllAsync(data, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = MULTI)
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    @Override
    public RemoteCacheClientStatisticsMXBean clientStatistics() {
        return delegate.clientStatistics();
    }

    @Override
    public ServerStatistics serverStatistics() {
        return delegate.serverStatistics();
    }

    @Override
    public CompletionStage<ServerStatistics> serverStatisticsAsync() {
        return delegate.serverStatisticsAsync();
    }

    @Override
    public RemoteCache<K, V> withFlags(Flag... flags) {
        return delegate.withFlags(flags);
    }

    @Override
    public RemoteCache<K, V> noFlags() {
        return delegate.noFlags();
    }

    @Override
    public Set<Flag> flags() {
        return delegate.flags();
    }

    @Override
    public RemoteCacheContainer getRemoteCacheContainer() {
        return delegate.getRemoteCacheContainer();
    }

    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    @Override
    public RemoteCacheManager getRemoteCacheManager() {
        return delegate.getRemoteCacheManager();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = MULTI)
    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        return delegate.getAll(keys);
    }

    @Override
    public String getProtocolVersion() {
        return delegate.getProtocolVersion();
    }

    @Override
    public void addClientListener(Object listener) {
        delegate.addClientListener(listener);
    }

    @Override
    public void addClientListener(Object listener, Object[] filterFactoryParams, Object[] converterFactoryParams) {
        delegate.addClientListener(listener, filterFactoryParams, converterFactoryParams);
    }

    @Override
    public void removeClientListener(Object listener) {
        delegate.removeClientListener(listener);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public <T> T execute(String taskName) {
        return delegate.execute(taskName);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public <T> T execute(String taskName, Map<String, ?> params) {
        return delegate.execute(taskName, params);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public <T> T execute(String taskName, Map<String, ?> params, Object key) {
        return delegate.execute(taskName, params, key);
    }

    @Override
    public CacheTopologyInfo getCacheTopologyInfo() {
        return delegate.getCacheTopologyInfo();
    }

    @Override
    public StreamingRemoteCache<K> streaming() {
        return delegate.streaming();
    }

    @Override
    public <T, U> RemoteCache<T, U> withDataFormat(DataFormat dataFormat) {
        return delegate.withDataFormat(dataFormat);
    }

    @Override
    public DataFormat getDataFormat() {
        return delegate.getDataFormat();
    }

    @Override
    public boolean isTransactional() {
        return delegate.isTransactional();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.put(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.putIfAbsent(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.replace(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.put(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.putIfAbsent(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.replace(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit) {
        return delegate.merge(key, value, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.merge(key, value, remappingFunction, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit) {
        return delegate.compute(key, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.compute(key, remappingFunction, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit) {
        return delegate.computeIfPresent(key, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan,
            TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.computeIfPresent(key, remappingFunction, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return delegate.computeIfAbsent(key, mappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit,
            long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return delegate.computeIfAbsent(key, mappingFunction, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public <T> org.infinispan.commons.api.query.Query<T> query(String query) {
        return delegate.query(query);
    }

    @Override
    public ContinuousQuery<K, V> continuousQuery() {
        return delegate.continuousQuery();
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putAsync(K key, V value) {
        return delegate.putAsync(key, value);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.putAsync(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
            TimeUnit maxIdleUnit) {
        return delegate.putAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = CACHE_WIDE)
    @Override
    public CompletableFuture<Void> clearAsync() {
        return delegate.clearAsync();
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Long> sizeAsync() {
        return delegate.sizeAsync();
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value) {
        return delegate.putIfAbsentAsync(key, value);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.putIfAbsentAsync(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
            TimeUnit maxIdleUnit) {
        return delegate.putIfAbsentAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> removeAsync(Object key) {
        return delegate.removeAsync(key);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
        return delegate.removeAsync(key, value);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> replaceAsync(K key, V value) {
        return delegate.replaceAsync(key, value);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit unit) {
        return delegate.replaceAsync(key, value, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle,
            TimeUnit maxIdleUnit) {
        return delegate.replaceAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        return delegate.replaceAsync(key, oldValue, newValue);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
        return delegate.replaceAsync(key, oldValue, newValue, lifespan, unit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit,
            long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> getAsync(K key) {
        return delegate.getAsync(key);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<Boolean> containsKeyAsync(K key) {
        return delegate.containsKeyAsync(key);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = MULTI)
    @Override
    public CompletableFuture<Map<K, V>> getAllAsync(Set<?> keys) {
        return delegate.getAllAsync(keys);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeAsync(key, remappingFunction);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit) {
        return delegate.computeAsync(key, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.computeAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsentAsync(key, mappingFunction);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction, long lifespan,
            TimeUnit lifespanUnit) {
        return delegate.computeIfAbsentAsync(key, mappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction, long lifespan,
            TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.computeIfAbsentAsync(key, mappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresentAsync(key, remappingFunction);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit) {
        return delegate.computeIfPresentAsync(key, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.computeIfPresentAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.mergeAsync(key, value, remappingFunction);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit) {
        return delegate.mergeAsync(key, value, remappingFunction, lifespan, lifespanUnit);
    }

    @JfrCacheOperation(executionMode = ASYNC, scope = SINGLE)
    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction,
            long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return delegate.mergeAsync(key, value, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V replace(K key, V value) {
        return delegate.replace(key, value);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        delegate.replaceAll(function);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public int size() {
        return delegate.size();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = SINGLE)
    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    @JfrCacheOperation(executionMode = SYNC, scope = CACHE_WIDE)
    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public TransactionManager getTransactionManager() {
        return delegate.getTransactionManager();
    }
}
