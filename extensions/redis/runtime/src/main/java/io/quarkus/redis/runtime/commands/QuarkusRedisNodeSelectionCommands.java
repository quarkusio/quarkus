package io.quarkus.redis.runtime.commands;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.Consumer;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.KillArgs;
import io.lettuce.core.Limit;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.MigrateArgs;
import io.lettuce.core.Range;
import io.lettuce.core.RestoreArgs;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.SortArgs;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.UnblockType;
import io.lettuce.core.Value;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

public class QuarkusRedisNodeSelectionCommands<K, V> implements NodeSelectionCommands<K, V> {
    private final NodeSelectionAsyncCommands<K, V> nodeSelectionAsyncCommands;

    public QuarkusRedisNodeSelectionCommands(NodeSelectionAsyncCommands<K, V> nodeSelectionAsyncCommands) {
        this.nodeSelectionAsyncCommands = nodeSelectionAsyncCommands;
    }

    @Override
    public Executions<Long> publish(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.publish(k, v));
    }

    @Override
    public Executions<List<K>> pubsubChannels() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pubsubChannels());
    }

    @Override
    public Executions<List<K>> pubsubChannels(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pubsubChannels(k));
    }

    @Override
    public final Executions<Map<K, Long>> pubsubNumsub(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pubsubNumsub(ks));
    }

    @Override
    public Executions<Long> pubsubNumpat() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pubsubNumpat());
    }

    @Override
    public Executions<V> echo(V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.echo(v));
    }

    @Override
    public Executions<List<Object>> role() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.role());
    }

    @Override
    public Executions<String> ping() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.ping());
    }

    @Override
    public Executions<String> quit() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.quit());
    }

    @Override
    public Executions<Long> waitForReplication(int i, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.waitForReplication(i, l));
    }

    public <T> Executions<T> dispatch(ProtocolKeyword protocolKeyword, CommandOutput<K, V, T> commandOutput) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.dispatch(protocolKeyword, commandOutput));
    }

    public <T> Executions<T> dispatch(ProtocolKeyword protocolKeyword, CommandOutput<K, V, T> commandOutput,
            CommandArgs<K, V> commandArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.dispatch(protocolKeyword, commandOutput, commandArgs));
    }

    @Override
    public Executions<Long> geoadd(K k, double v, double v1, V v2) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.geoadd(k, v, v1, v2));
    }

    @Override
    public Executions<Long> geoadd(K k, Object... objects) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.geoadd(k, objects));
    }

    @Override
    public Executions<List<Value<String>>> geohash(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.geohash(k, vs));
    }

    @Override
    public Executions<Set<V>> georadius(K k, double v, double v1, double v2, GeoArgs.Unit unit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadius(k, v, v1, v2, unit));
    }

    @Override
    public Executions<List<GeoWithin<V>>> georadius(K k, double v, double v1, double v2, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadius(k, v, v1, v2, unit, geoArgs));
    }

    @Override
    public Executions<Long> georadius(K k, double v, double v1, double v2, GeoArgs.Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadius(k, v, v1, v2, unit, geoRadiusStoreArgs));
    }

    @Override
    public Executions<Set<V>> georadiusbymember(K k, V v, double v1, GeoArgs.Unit unit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadiusbymember(k, v, v1, unit));
    }

    @Override
    public Executions<List<GeoWithin<V>>> georadiusbymember(K k, V v, double v1, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadiusbymember(k, v, v1, unit, geoArgs));
    }

    @Override
    public Executions<Long> georadiusbymember(K k, V v, double v1, GeoArgs.Unit unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.georadiusbymember(k, v, v1, unit, geoRadiusStoreArgs));
    }

    @Override
    public Executions<List<GeoCoordinates>> geopos(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.geopos(k, vs));
    }

    @Override
    public Executions<Double> geodist(K k, V v, V v1, GeoArgs.Unit unit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.geodist(k, v, v1, unit));
    }

    @Override
    public Executions<Long> hdel(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hdel(k, ks));
    }

    @Override
    public Executions<Boolean> hexists(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hexists(k, k1));
    }

    @Override
    public Executions<V> hget(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hget(k, k1));
    }

    @Override
    public Executions<Long> hincrby(K k, K k1, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hincrby(k, k1, l));
    }

    @Override
    public Executions<Double> hincrbyfloat(K k, K k1, double v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hincrbyfloat(k, k1, v));
    }

    @Override
    public Executions<Map<K, V>> hgetall(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hgetall(k));
    }

    @Override
    public Executions<Long> hgetall(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hgetall(keyValueStreamingChannel, k));
    }

    @Override
    public Executions<List<K>> hkeys(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hkeys(k));
    }

    @Override
    public Executions<Long> hkeys(KeyStreamingChannel<K> keyStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hkeys(keyStreamingChannel, k));
    }

    @Override
    public Executions<Long> hlen(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hlen(k));
    }

    @Override
    public Executions<List<KeyValue<K, V>>> hmget(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hmget(k, ks));
    }

    @Override
    public Executions<Long> hmget(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hmget(keyValueStreamingChannel, k, ks));
    }

    @Override
    public Executions<String> hmset(K k, Map<K, V> map) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hmset(k, map));
    }

    @Override
    public Executions<MapScanCursor<K, V>> hscan(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(k));
    }

    @Override
    public Executions<MapScanCursor<K, V>> hscan(K k, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(k, scanArgs));
    }

    @Override
    public Executions<MapScanCursor<K, V>> hscan(K k, ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(k, scanCursor, scanArgs));
    }

    @Override
    public Executions<MapScanCursor<K, V>> hscan(K k, ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(k, scanCursor));
    }

    @Override
    public Executions<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(keyValueStreamingChannel, k));
    }

    @Override
    public Executions<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(keyValueStreamingChannel, k, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k,
            ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.hscan(keyValueStreamingChannel, k, scanCursor, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K k,
            ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hscan(keyValueStreamingChannel, k, scanCursor));
    }

    @Override
    public Executions<Boolean> hset(K k, K k1, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hset(k, k1, v));
    }

    @Override
    public Executions<Boolean> hsetnx(K k, K k1, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hsetnx(k, k1, v));
    }

    @Override
    public Executions<Long> hstrlen(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hstrlen(k, k1));
    }

    @Override
    public Executions<List<V>> hvals(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hvals(k));
    }

    @Override
    public Executions<Long> hvals(ValueStreamingChannel<V> valueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.hvals(valueStreamingChannel, k));
    }

    @Override
    public Executions<Long> pfadd(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pfadd(k, vs));
    }

    @Override
    public Executions<String> pfmerge(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pfmerge(k, ks));
    }

    @Override
    public Executions<Long> pfcount(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pfcount(ks));
    }

    @Override
    public Executions<Long> del(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.del(ks));
    }

    @Override
    public Executions<Long> unlink(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.unlink(ks));
    }

    @Override
    public Executions<byte[]> dump(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.dump(k));
    }

    @Override
    public Executions<Long> exists(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.exists(ks));
    }

    @Override
    public Executions<Boolean> expire(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.expire(k, l));
    }

    @Override
    public Executions<Boolean> expireat(K k, Date date) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.expireat(k, date));
    }

    @Override
    public Executions<Boolean> expireat(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.expireat(k, l));
    }

    @Override
    public Executions<List<K>> keys(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.keys(k));
    }

    @Override
    public Executions<Long> keys(KeyStreamingChannel<K> keyStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.keys(keyStreamingChannel, k));
    }

    @Override
    public Executions<String> migrate(String s, int i, K k, int i1, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.migrate(s, i, k, i1, l));
    }

    @Override
    public Executions<String> migrate(String s, int i, int i1, long l, MigrateArgs<K> migrateArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.migrate(s, i, i1, l, migrateArgs));
    }

    @Override
    public Executions<Boolean> move(K k, int i) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.move(k, i));
    }

    @Override
    public Executions<String> objectEncoding(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.objectEncoding(k));
    }

    @Override
    public Executions<Long> objectIdletime(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.objectIdletime(k));
    }

    @Override
    public Executions<Long> objectRefcount(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.objectRefcount(k));
    }

    @Override
    public Executions<Boolean> persist(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.persist(k));
    }

    @Override
    public Executions<Boolean> pexpire(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pexpire(k, l));
    }

    @Override
    public Executions<Boolean> pexpireat(K k, Date date) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pexpireat(k, date));
    }

    @Override
    public Executions<Boolean> pexpireat(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pexpireat(k, l));
    }

    @Override
    public Executions<Long> pttl(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.pttl(k));
    }

    @Override
    public Executions<V> randomkey() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.randomkey());
    }

    @Override
    public Executions<String> rename(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.rename(k, k1));
    }

    @Override
    public Executions<Boolean> renamenx(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.renamenx(k, k1));
    }

    @Override
    public Executions<String> restore(K k, long l, byte[] bytes) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.restore(k, l, bytes));
    }

    @Override
    public Executions<String> restore(K k, byte[] bytes, RestoreArgs restoreArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.restore(k, bytes, restoreArgs));
    }

    @Override
    public Executions<List<V>> sort(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sort(k));
    }

    @Override
    public Executions<Long> sort(ValueStreamingChannel<V> valueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sort(valueStreamingChannel, k));
    }

    @Override
    public Executions<List<V>> sort(K k, SortArgs sortArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sort(k, sortArgs));
    }

    @Override
    public Executions<Long> sort(ValueStreamingChannel<V> valueStreamingChannel, K k, SortArgs sortArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sort(valueStreamingChannel, k, sortArgs));
    }

    @Override
    public Executions<Long> sortStore(K k, SortArgs sortArgs, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sortStore(k, sortArgs, k1));
    }

    @Override
    public Executions<Long> touch(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.touch(ks));
    }

    @Override
    public Executions<Long> ttl(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.ttl(k));
    }

    @Override
    public Executions<String> type(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.type(k));
    }

    @Override
    public Executions<KeyScanCursor<K>> scan() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan());
    }

    @Override
    public Executions<KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(scanArgs));
    }

    @Override
    public Executions<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(scanCursor, scanArgs));
    }

    @Override
    public Executions<KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(scanCursor));
    }

    @Override
    public Executions<StreamScanCursor> scan(KeyStreamingChannel<K> keyStreamingChannel) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(keyStreamingChannel));
    }

    @Override
    public Executions<StreamScanCursor> scan(KeyStreamingChannel<K> keyStreamingChannel, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(keyStreamingChannel, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> scan(KeyStreamingChannel<K> keyStreamingChannel, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(keyStreamingChannel, scanCursor, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> scan(KeyStreamingChannel<K> keyStreamingChannel, ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scan(keyStreamingChannel, scanCursor));
    }

    @Override
    public Executions<KeyValue<K, V>> blpop(long l, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.blpop(l, ks));
    }

    @Override
    public Executions<KeyValue<K, V>> brpop(long l, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.brpop(l, ks));
    }

    @Override
    public Executions<V> brpoplpush(long l, K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.brpoplpush(l, k, k1));
    }

    @Override
    public Executions<V> lindex(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lindex(k, l));
    }

    @Override
    public Executions<Long> linsert(K k, boolean b, V v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.linsert(k, b, v, v1));
    }

    @Override
    public Executions<Long> llen(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.llen(k));
    }

    @Override
    public Executions<V> lpop(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lpop(k));
    }

    @Override
    public Executions<Long> lpush(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lpush(k, vs));
    }

    @Override
    public Executions<Long> lpushx(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lpushx(k, vs));
    }

    @Override
    public Executions<List<V>> lrange(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lrange(k, l, l1));
    }

    @Override
    public Executions<Long> lrange(ValueStreamingChannel<V> valueStreamingChannel, K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lrange(valueStreamingChannel, k, l, l1));
    }

    @Override
    public Executions<Long> lrem(K k, long l, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lrem(k, l, v));
    }

    @Override
    public Executions<String> lset(K k, long l, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lset(k, l, v));
    }

    @Override
    public Executions<String> ltrim(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.ltrim(k, l, l1));
    }

    @Override
    public Executions<V> rpop(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.rpop(k));
    }

    @Override
    public Executions<V> rpoplpush(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.rpoplpush(k, k1));
    }

    @Override
    public Executions<Long> rpush(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.rpush(k, vs));
    }

    @Override
    public Executions<Long> rpushx(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.rpushx(k, vs));
    }

    @Override
    public <T> Executions<T> eval(String s, ScriptOutputType scriptOutputType, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.eval(s, scriptOutputType, ks));
    }

    @Override
    public <T> Executions<T> eval(String s, ScriptOutputType scriptOutputType, K[] ks, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.eval(s, scriptOutputType, ks, vs));
    }

    @Override
    public <T> Executions<T> evalsha(String s, ScriptOutputType scriptOutputType, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.evalsha(s, scriptOutputType, ks));
    }

    @Override
    public <T> Executions<T> evalsha(String s, ScriptOutputType scriptOutputType, K[] ks, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.evalsha(s, scriptOutputType, ks, vs));
    }

    @Override
    public Executions<List<Boolean>> scriptExists(String... strings) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scriptExists(strings));
    }

    @Override
    public Executions<String> scriptFlush() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scriptFlush());
    }

    @Override
    public Executions<String> scriptKill() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scriptKill());
    }

    @Override
    public Executions<String> scriptLoad(V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scriptLoad(v));
    }

    @Override
    public Executions<String> bgrewriteaof() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bgrewriteaof());
    }

    @Override
    public Executions<String> bgsave() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bgsave());
    }

    @Override
    public Executions<K> clientGetname() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientGetname());
    }

    @Override
    public Executions<String> clientSetname(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientSetname(k));
    }

    @Override
    public Executions<String> clientKill(String s) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientKill(s));
    }

    @Override
    public Executions<Long> clientKill(KillArgs killArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientKill(killArgs));
    }

    @Override
    public Executions<Long> clientUnblock(long l, UnblockType unblockType) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientUnblock(l, unblockType));
    }

    @Override
    public Executions<String> clientPause(long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientPause(l));
    }

    @Override
    public Executions<String> clientList() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.clientList());
    }

    @Override
    public Executions<List<Object>> command() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.command());
    }

    @Override
    public Executions<List<Object>> commandInfo(String... strings) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.commandInfo(strings));
    }

    @Override
    public Executions<List<Object>> commandInfo(CommandType... commandTypes) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.commandInfo(commandTypes));
    }

    @Override
    public Executions<Long> commandCount() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.commandCount());
    }

    @Override
    public Executions<Map<String, String>> configGet(String s) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.configGet(s));
    }

    @Override
    public Executions<String> configResetstat() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.configResetstat());
    }

    @Override
    public Executions<String> configRewrite() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.configRewrite());
    }

    @Override
    public Executions<String> configSet(String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.configSet(s, s1));
    }

    @Override
    public Executions<Long> dbsize() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.dbsize());
    }

    @Override
    public Executions<String> debugCrashAndRecover(Long aLong) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugCrashAndRecover(aLong));
    }

    @Override
    public Executions<String> debugHtstats(int i) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugHtstats(i));
    }

    @Override
    public Executions<String> debugObject(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugObject(k));
    }

    @Override
    public Executions<String> debugReload() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugReload());
    }

    @Override
    public Executions<String> debugRestart(Long aLong) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugRestart(aLong));
    }

    @Override
    public Executions<String> debugSdslen(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.debugSdslen(k));
    }

    @Override
    public Executions<String> flushall() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.flushall());
    }

    @Override
    public Executions<String> flushallAsync() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.flushallAsync());
    }

    @Override
    public Executions<String> flushdb() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.flushdb());
    }

    @Override
    public Executions<String> flushdbAsync() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.flushdbAsync());
    }

    @Override
    public Executions<String> info() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.info());
    }

    @Override
    public Executions<String> info(String s) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.info(s));
    }

    @Override
    public Executions<Date> lastsave() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.lastsave());
    }

    @Override
    public Executions<Long> memoryUsage(K key) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.memoryUsage(key));
    }

    @Override
    public Executions<String> save() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.save());
    }

    @Override
    public Executions<String> slaveof(String s, int i) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slaveof(s, i));
    }

    @Override
    public Executions<String> slaveofNoOne() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slaveofNoOne());
    }

    @Override
    public Executions<List<Object>> slowlogGet() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slowlogGet());
    }

    @Override
    public Executions<List<Object>> slowlogGet(int i) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slowlogGet(i));
    }

    @Override
    public Executions<Long> slowlogLen() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slowlogLen());
    }

    @Override
    public Executions<String> slowlogReset() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.slowlogReset());
    }

    @Override
    public Executions<List<V>> time() {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.time());
    }

    @Override
    public Executions<Long> sadd(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sadd(k, vs));
    }

    @Override
    public Executions<Long> scard(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.scard(k));
    }

    @Override
    public Executions<Set<V>> sdiff(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sdiff(ks));
    }

    @Override
    public Executions<Long> sdiff(ValueStreamingChannel<V> valueStreamingChannel, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sdiff(valueStreamingChannel, ks));
    }

    @Override
    public Executions<Long> sdiffstore(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sdiffstore(k, ks));
    }

    @Override
    public Executions<Set<V>> sinter(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sinter(ks));
    }

    @Override
    public Executions<Long> sinter(ValueStreamingChannel<V> valueStreamingChannel, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sinter(valueStreamingChannel, ks));
    }

    @Override
    public Executions<Long> sinterstore(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sinterstore(k, ks));
    }

    @Override
    public Executions<Boolean> sismember(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sismember(k, v));
    }

    @Override
    public Executions<Boolean> smove(K k, K k1, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.smove(k, k1, v));
    }

    @Override
    public Executions<Set<V>> smembers(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.smembers(k));
    }

    @Override
    public Executions<Long> smembers(ValueStreamingChannel<V> valueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.smembers(valueStreamingChannel, k));
    }

    @Override
    public Executions<V> spop(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.spop(k));
    }

    @Override
    public Executions<Set<V>> spop(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.spop(k, l));
    }

    @Override
    public Executions<V> srandmember(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.srandmember(k));
    }

    @Override
    public Executions<List<V>> srandmember(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.srandmember(k, l));
    }

    @Override
    public Executions<Long> srandmember(ValueStreamingChannel<V> valueStreamingChannel, K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.srandmember(valueStreamingChannel, k, l));
    }

    @Override
    public Executions<Long> srem(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.srem(k, vs));
    }

    @Override
    public Executions<Set<V>> sunion(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sunion(ks));
    }

    @Override
    public Executions<Long> sunion(ValueStreamingChannel<V> valueStreamingChannel, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sunion(valueStreamingChannel, ks));
    }

    @Override
    public Executions<Long> sunionstore(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sunionstore(k, ks));
    }

    @Override
    public Executions<ValueScanCursor<V>> sscan(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(k));
    }

    @Override
    public Executions<ValueScanCursor<V>> sscan(K k, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(k, scanArgs));
    }

    @Override
    public Executions<ValueScanCursor<V>> sscan(K k, ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(k, scanCursor, scanArgs));
    }

    @Override
    public Executions<ValueScanCursor<V>> sscan(K k, ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(k, scanCursor));
    }

    @Override
    public Executions<StreamScanCursor> sscan(ValueStreamingChannel<V> valueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(valueStreamingChannel, k));
    }

    @Override
    public Executions<StreamScanCursor> sscan(ValueStreamingChannel<V> valueStreamingChannel, K k, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(valueStreamingChannel, k, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> sscan(ValueStreamingChannel<V> valueStreamingChannel, K k, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(valueStreamingChannel, k, scanCursor, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> sscan(ValueStreamingChannel<V> valueStreamingChannel, K k, ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.sscan(valueStreamingChannel, k, scanCursor));
    }

    @Override
    public Executions<KeyValue<K, ScoredValue<V>>> bzpopmin(long l, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bzpopmin(l, ks));
    }

    @Override
    public Executions<KeyValue<K, ScoredValue<V>>> bzpopmax(long l, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bzpopmax(l, ks));
    }

    @Override
    public Executions<Long> zadd(K k, double v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, v, v1));
    }

    @Override
    public Executions<Long> zadd(K k, Object... objects) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, objects));
    }

    @Override
    public Executions<Long> zadd(K k, ScoredValue<V>... scoredValues) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, scoredValues));
    }

    @Override
    public Executions<Long> zadd(K k, ZAddArgs zAddArgs, double v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, zAddArgs, v, v1));
    }

    @Override
    public Executions<Long> zadd(K k, ZAddArgs zAddArgs, Object... objects) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, zAddArgs, objects));
    }

    @Override
    public Executions<Long> zadd(K k, ZAddArgs zAddArgs, ScoredValue<V>... scoredValues) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zadd(k, zAddArgs, scoredValues));
    }

    @Override
    public Executions<Double> zaddincr(K k, double v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zaddincr(k, v, v1));
    }

    @Override
    public Executions<Double> zaddincr(K k, ZAddArgs zAddArgs, double v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zaddincr(k, zAddArgs, v, v1));
    }

    @Override
    public Executions<Long> zcard(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zcard(k));
    }

    @Override
    @Deprecated
    public Executions<Long> zcount(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zcount(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zcount(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zcount(k, s, s1));
    }

    @Override
    public Executions<Long> zcount(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zcount(k, range));
    }

    @Override
    public Executions<Double> zincrby(K k, double v, V v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zincrby(k, v, v1));
    }

    @Override
    public Executions<Long> zinterstore(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zinterstore(k, ks));
    }

    @Override
    public Executions<Long> zinterstore(K k, ZStoreArgs zStoreArgs, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zinterstore(k, zStoreArgs, ks));
    }

    @Override
    @Deprecated
    public Executions<Long> zlexcount(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zlexcount(k, s, s1));
    }

    @Override
    public Executions<Long> zlexcount(K k, Range<? extends V> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zlexcount(k, range));
    }

    @Override
    public Executions<ScoredValue<V>> zpopmin(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zpopmin(k));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zpopmin(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zpopmin(k, l));
    }

    @Override
    public Executions<ScoredValue<V>> zpopmax(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zpopmax(k));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zpopmax(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zpopmax(k, l));
    }

    @Override
    public Executions<List<V>> zrange(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrange(k, l, l1));
    }

    @Override
    public Executions<Long> zrange(ValueStreamingChannel<V> valueStreamingChannel, K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrange(valueStreamingChannel, k, l, l1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrangeWithScores(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangeWithScores(k, l, l1));
    }

    @Override
    public Executions<Long> zrangeWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangeWithScores(scoredValueStreamingChannel, k, l, l1));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebylex(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebylex(k, s, s1));
    }

    @Override
    public Executions<List<V>> zrangebylex(K k, Range<? extends V> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebylex(k, range));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebylex(K k, String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebylex(k, s, s1, l, l1));
    }

    @Override
    public Executions<List<V>> zrangebylex(K k, Range<? extends V> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebylex(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebyscore(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebyscore(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, s, s1));
    }

    @Override
    public Executions<List<V>> zrangebyscore(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, range));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebyscore(K k, double v, double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrangebyscore(K k, String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, s, s1, l, l1));
    }

    @Override
    public Executions<List<V>> zrangebyscore(K k, Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, s, s1));
    }

    @Override
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, range));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, double v, double v1, long l,
            long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, String s, String s1, long l,
            long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, s, s1, l, l1));
    }

    @Override
    public Executions<Long> zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, Range<? extends Number> range,
            Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscore(valueStreamingChannel, k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, s, s1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, range));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, double v, double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, s, s1, l, l1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrangebyscoreWithScores(K k, Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrangebyscoreWithScores(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, double v,
            double v1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, String s,
            String s1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, s, s1));
    }

    @Override
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, range));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, double v,
            double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, String s,
            String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, s, s1, l, l1));
    }

    @Override
    public Executions<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, k, range, limit));
    }

    @Override
    public Executions<Long> zrank(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrank(k, v));
    }

    @Override
    public Executions<Long> zrem(K k, V... vs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrem(k, vs));
    }

    @Override
    @Deprecated
    public Executions<Long> zremrangebylex(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebylex(k, s, s1));
    }

    @Override
    public Executions<Long> zremrangebylex(K k, Range<? extends V> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebylex(k, range));
    }

    @Override
    public Executions<Long> zremrangebyrank(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebyrank(k, l, l1));
    }

    @Override
    @Deprecated
    public Executions<Long> zremrangebyscore(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebyscore(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zremrangebyscore(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebyscore(k, s, s1));
    }

    @Override
    public Executions<Long> zremrangebyscore(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zremrangebyscore(k, range));
    }

    @Override
    public Executions<List<V>> zrevrange(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrange(k, l, l1));
    }

    @Override
    public Executions<Long> zrevrange(ValueStreamingChannel<V> valueStreamingChannel, K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrange(valueStreamingChannel, k, l, l1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrevrangeWithScores(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangeWithScores(k, l, l1));
    }

    @Override
    public Executions<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k, long l,
            long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangeWithScores(scoredValueStreamingChannel, k, l, l1));
    }

    @Override
    public Executions<List<V>> zrevrangebylex(K k, Range<? extends V> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebylex(k, range));
    }

    @Override
    public Executions<List<V>> zrevrangebylex(K k, Range<? extends V> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebylex(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrevrangebyscore(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrevrangebyscore(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, s, s1));
    }

    @Override
    public Executions<List<V>> zrevrangebyscore(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, range));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrevrangebyscore(K k, double v, double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<List<V>> zrevrangebyscore(K k, String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, s, s1, l, l1));
    }

    @Override
    public Executions<List<V>> zrevrangebyscore(K k, Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, s, s1));
    }

    @Override
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k,
            Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, range));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, double v, double v1, long l,
            long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, String s, String s1, long l,
            long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, s, s1, l, l1));
    }

    @Override
    public Executions<Long> zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K k, Range<? extends Number> range,
            Limit limit) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscore(valueStreamingChannel, k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, double v, double v1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, String s, String s1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, s, s1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, range));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, double v, double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, s, s1, l, l1));
    }

    @Override
    public Executions<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K k, Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(k, range, limit));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            double v, double v1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, v, v1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            String s, String s1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, s, s1));
    }

    @Override
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            Range<? extends Number> range) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, range));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            double v, double v1, long l, long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            String s, String s1, long l, long l1) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, s, s1, l, l1));
    }

    @Override
    public Executions<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            Range<? extends Number> range, Limit limit) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, k, range, limit));
    }

    @Override
    public Executions<Long> zrevrank(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zrevrank(k, v));
    }

    @Override
    public Executions<ScoredValueScanCursor<V>> zscan(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(k));
    }

    @Override
    public Executions<ScoredValueScanCursor<V>> zscan(K k, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(k, scanArgs));
    }

    @Override
    public Executions<ScoredValueScanCursor<V>> zscan(K k, ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(k, scanCursor, scanArgs));
    }

    @Override
    public Executions<ScoredValueScanCursor<V>> zscan(K k, ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(k, scanCursor));
    }

    @Override
    public Executions<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(scoredValueStreamingChannel, k));
    }

    @Override
    public Executions<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(scoredValueStreamingChannel, k, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            ScanCursor scanCursor, ScanArgs scanArgs) {
        return new QuarkusRedisExecutions<>(
                nodeSelectionAsyncCommands.zscan(scoredValueStreamingChannel, k, scanCursor, scanArgs));
    }

    @Override
    public Executions<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K k,
            ScanCursor scanCursor) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscan(scoredValueStreamingChannel, k, scanCursor));
    }

    @Override
    public Executions<Double> zscore(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zscore(k, v));
    }

    @Override
    public Executions<Long> zunionstore(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zunionstore(k, ks));
    }

    @Override
    public Executions<Long> zunionstore(K k, ZStoreArgs zStoreArgs, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.zunionstore(k, zStoreArgs, ks));
    }

    @Override
    public Executions<Long> xack(K k, K k1, String... strings) {
        return nodeSelectionAsyncCommands.xack(k, k1, strings);
    }

    @Override
    public Executions<String> xadd(K k, Map<K, V> map) {
        return nodeSelectionAsyncCommands.xadd(k, map);
    }

    @Override
    public Executions<String> xadd(K k, XAddArgs xAddArgs, Map<K, V> map) {
        return nodeSelectionAsyncCommands.xadd(k, xAddArgs, map);
    }

    @Override
    public Executions<String> xadd(K k, Object... objects) {
        return nodeSelectionAsyncCommands.xadd(k, objects);
    }

    @Override
    public Executions<String> xadd(K k, XAddArgs xAddArgs, Object... objects) {
        return nodeSelectionAsyncCommands.xadd(k, xAddArgs, objects);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xclaim(K k, Consumer<K> consumer, long l, String... strings) {
        return nodeSelectionAsyncCommands.xclaim(k, consumer, l, strings);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xclaim(K k, Consumer<K> consumer, XClaimArgs xClaimArgs, String... strings) {
        return nodeSelectionAsyncCommands.xclaim(k, consumer, xClaimArgs, strings);
    }

    @Override
    public Executions<Long> xdel(K k, String... strings) {
        return nodeSelectionAsyncCommands.xdel(k, strings);
    }

    @Override
    public Executions<String> xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K k) {
        return nodeSelectionAsyncCommands.xgroupCreate(streamOffset, k);
    }

    @Override
    public Executions<String> xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K group, XGroupCreateArgs args) {
        return nodeSelectionAsyncCommands.xgroupCreate(streamOffset, group, args);
    }

    @Override
    public Executions<Boolean> xgroupDelconsumer(K k, Consumer<K> consumer) {
        return nodeSelectionAsyncCommands.xgroupDelconsumer(k, consumer);
    }

    @Override
    public Executions<Boolean> xgroupDestroy(K k, K k1) {
        return nodeSelectionAsyncCommands.xgroupDestroy(k, k1);
    }

    @Override
    public Executions<String> xgroupSetid(XReadArgs.StreamOffset<K> streamOffset, K k) {
        return nodeSelectionAsyncCommands.xgroupSetid(streamOffset, k);
    }

    @Override
    public Executions<List<Object>> xinfoStream(K key) {
        return nodeSelectionAsyncCommands.xinfoStream(key);
    }

    @Override
    public Executions<List<Object>> xinfoGroups(K key) {
        return nodeSelectionAsyncCommands.xinfoGroups(key);
    }

    @Override
    public Executions<List<Object>> xinfoConsumers(K key, K group) {
        return nodeSelectionAsyncCommands.xinfoConsumers(key, group);
    }

    @Override
    public Executions<Long> xlen(K k) {
        return nodeSelectionAsyncCommands.xlen(k);
    }

    @Override
    public Executions<List<Object>> xpending(K k, K k1) {
        return nodeSelectionAsyncCommands.xpending(k, k1);
    }

    @Override
    public Executions<List<Object>> xpending(K k, K k1, Range<String> range, Limit limit) {
        return nodeSelectionAsyncCommands.xpending(k, k1, range, limit);
    }

    @Override
    public Executions<List<Object>> xpending(K k, Consumer<K> consumer, Range<String> range, Limit limit) {
        return nodeSelectionAsyncCommands.xpending(k, consumer, range, limit);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xrange(K k, Range<String> range) {
        return nodeSelectionAsyncCommands.xrange(k, range);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xrange(K k, Range<String> range, Limit limit) {
        return nodeSelectionAsyncCommands.xrange(k, range, limit);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xread(XReadArgs.StreamOffset<K>... streamOffsets) {
        return nodeSelectionAsyncCommands.xread(streamOffsets);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xread(XReadArgs xReadArgs, XReadArgs.StreamOffset<K>... streamOffsets) {
        return nodeSelectionAsyncCommands.xread(xReadArgs, streamOffsets);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xreadgroup(Consumer<K> consumer, XReadArgs.StreamOffset<K>... streamOffsets) {
        return nodeSelectionAsyncCommands.xreadgroup(consumer, streamOffsets);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xreadgroup(Consumer<K> consumer, XReadArgs xReadArgs,
            XReadArgs.StreamOffset<K>... streamOffsets) {
        return nodeSelectionAsyncCommands.xreadgroup(consumer, xReadArgs, streamOffsets);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xrevrange(K k, Range<String> range) {
        return nodeSelectionAsyncCommands.xrevrange(k, range);
    }

    @Override
    public Executions<List<StreamMessage<K, V>>> xrevrange(K k, Range<String> range, Limit limit) {
        return nodeSelectionAsyncCommands.xrevrange(k, range, limit);
    }

    @Override
    public Executions<Long> xtrim(K k, long l) {
        return nodeSelectionAsyncCommands.xtrim(k, l);
    }

    @Override
    public Executions<Long> xtrim(K k, boolean b, long l) {
        return nodeSelectionAsyncCommands.xtrim(k, b, l);
    }

    @Override
    public Executions<Long> append(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.append(k, v));
    }

    @Override
    public Executions<Long> bitcount(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitcount(k));
    }

    @Override
    public Executions<Long> bitcount(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitcount(k, l, l1));
    }

    @Override
    public Executions<List<Long>> bitfield(K k, BitFieldArgs bitFieldArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitfield(k, bitFieldArgs));
    }

    @Override
    public Executions<Long> bitpos(K k, boolean b) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitpos(k, b));
    }

    @Override
    public Executions<Long> bitpos(K k, boolean b, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitpos(k, b, l));
    }

    @Override
    public Executions<Long> bitpos(K k, boolean b, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitpos(k, b, l, l1));
    }

    @Override
    public Executions<Long> bitopAnd(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitopAnd(k, ks));
    }

    @Override
    public Executions<Long> bitopNot(K k, K k1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitopNot(k, k1));
    }

    @Override
    public Executions<Long> bitopOr(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitopOr(k, ks));
    }

    @Override
    public Executions<Long> bitopXor(K k, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.bitopXor(k, ks));
    }

    @Override
    public Executions<Long> decr(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.decr(k));
    }

    @Override
    public Executions<Long> decrby(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.decrby(k, l));
    }

    @Override
    public Executions<V> get(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.get(k));
    }

    @Override
    public Executions<Long> getbit(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.getbit(k, l));
    }

    @Override
    public Executions<V> getrange(K k, long l, long l1) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.getrange(k, l, l1));
    }

    @Override
    public Executions<V> getset(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.getset(k, v));
    }

    @Override
    public Executions<Long> incr(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.incr(k));
    }

    @Override
    public Executions<Long> incrby(K k, long l) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.incrby(k, l));
    }

    @Override
    public Executions<Double> incrbyfloat(K k, double v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.incrbyfloat(k, v));
    }

    @Override
    public Executions<List<KeyValue<K, V>>> mget(K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.mget(ks));
    }

    @Override
    public Executions<Long> mget(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K... ks) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.mget(keyValueStreamingChannel, ks));
    }

    @Override
    public Executions<String> mset(Map<K, V> map) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.mset(map));
    }

    @Override
    public Executions<Boolean> msetnx(Map<K, V> map) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.msetnx(map));
    }

    @Override
    public Executions<String> set(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.set(k, v));
    }

    @Override
    public Executions<String> set(K k, V v, SetArgs setArgs) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.set(k, v, setArgs));
    }

    @Override
    public Executions<Long> setbit(K k, long l, int i) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.setbit(k, l, i));
    }

    @Override
    public Executions<String> setex(K k, long l, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.setex(k, l, v));
    }

    @Override
    public Executions<String> psetex(K k, long l, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.psetex(k, l, v));
    }

    @Override
    public Executions<Boolean> setnx(K k, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.setnx(k, v));
    }

    @Override
    public Executions<Long> setrange(K k, long l, V v) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.setrange(k, l, v));
    }

    @Override
    public Executions<Long> strlen(K k) {
        return new QuarkusRedisExecutions<>(nodeSelectionAsyncCommands.strlen(k));
    }
}
