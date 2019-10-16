package io.quarkus.redis.runtime.commands;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.Consumer;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.KillArgs;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.Limit;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.MigrateArgs;
import io.lettuce.core.Range;
import io.lettuce.core.RedisFuture;
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
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.UnblockType;
import io.lettuce.core.Value;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.internal.TimeoutProvider;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

public class QuarkusRedisSyncCommand<K, V> implements RedisCommands<K, V> {
    private final RedisAsyncCommands<K, V> asyncCommands;
    private final TimeoutProvider timeoutProvider;

    public QuarkusRedisSyncCommand(RedisAsyncCommands<K, V> asyncCommands) {
        this.asyncCommands = asyncCommands;
        this.timeoutProvider = new TimeoutProvider(new Supplier<TimeoutOptions>() {
            @Override
            public TimeoutOptions get() {
                return getStatefulConnection().getOptions().getTimeoutOptions();
            }
        }, new LongSupplier() {
            @Override
            public long getAsLong() {
                return getStatefulConnection()
                        .getTimeout().toNanos();
            }
        });
    }

    @Override
    public void setTimeout(Duration duration) {
        asyncCommands.setTimeout(duration);
    }

    @Deprecated
    @Override
    public void setTimeout(long l, TimeUnit timeUnit) {
        asyncCommands.setTimeout(l, timeUnit);
    }

    @Override
    public String auth(String s) {
        return asyncCommands.auth(s);
    }

    @Override
    public String clusterBumpepoch() {
        return awaitOrCancel(asyncCommands.clusterBumpepoch());
    }

    @Override
    public String clusterMeet(String s, int i) {
        return awaitOrCancel(asyncCommands.clusterMeet(s, i));
    }

    @Override
    public String clusterForget(String s) {
        return awaitOrCancel(asyncCommands.clusterForget(s));
    }

    @Override
    public String clusterAddSlots(int... ints) {
        return awaitOrCancel(asyncCommands.clusterAddSlots(ints));
    }

    @Override
    public String clusterDelSlots(int... ints) {
        return awaitOrCancel(asyncCommands.clusterDelSlots(ints));
    }

    @Override
    public String clusterSetSlotNode(int i, String s) {
        return awaitOrCancel(asyncCommands.clusterSetSlotNode(i, s));
    }

    @Override
    public String clusterSetSlotStable(int i) {
        return awaitOrCancel(asyncCommands.clusterSetSlotStable(i));
    }

    @Override
    public String clusterSetSlotMigrating(int i, String s) {
        return awaitOrCancel(asyncCommands.clusterSetSlotMigrating(i, s));
    }

    @Override
    public String clusterSetSlotImporting(int i, String s) {
        return awaitOrCancel(asyncCommands.clusterSetSlotImporting(i, s));
    }

    @Override
    public String clusterInfo() {
        return awaitOrCancel(asyncCommands.clusterInfo());
    }

    @Override
    public String clusterMyId() {
        return awaitOrCancel(asyncCommands.clusterMyId());
    }

    @Override
    public String clusterNodes() {
        return awaitOrCancel(asyncCommands.clusterNodes());
    }

    @Override
    public List<String> clusterSlaves(String s) {
        return awaitOrCancel(asyncCommands.clusterSlaves(s));
    }

    @Override
    public List<K> clusterGetKeysInSlot(int i, int i1) {
        return awaitOrCancel(asyncCommands.clusterGetKeysInSlot(i, i1));
    }

    @Override
    public Long clusterCountKeysInSlot(int i) {
        return awaitOrCancel(asyncCommands.clusterCountKeysInSlot(i));
    }

    @Override
    public Long clusterCountFailureReports(String s) {
        return awaitOrCancel(asyncCommands.clusterCountFailureReports(s));
    }

    @Override
    public Long clusterKeyslot(K o) {
        return awaitOrCancel(asyncCommands.clusterKeyslot(o));
    }

    @Override
    public String clusterSaveconfig() {
        return awaitOrCancel(asyncCommands.clusterSaveconfig());
    }

    @Override
    public String clusterSetConfigEpoch(long l) {
        return awaitOrCancel(asyncCommands.clusterSetConfigEpoch(l));
    }

    @Override
    public List<Object> clusterSlots() {
        return awaitOrCancel(asyncCommands.clusterSlots());
    }

    @Override
    public String asking() {
        return awaitOrCancel(asyncCommands.asking());
    }

    @Override
    public String clusterReplicate(String s) {
        return awaitOrCancel(asyncCommands.clusterReplicate(s));
    }

    @Override
    public String clusterFailover(boolean b) {
        return awaitOrCancel(asyncCommands.clusterFailover(b));
    }

    @Override
    public String clusterReset(boolean b) {
        return awaitOrCancel(asyncCommands.clusterReset(b));
    }

    @Override
    public String clusterFlushslots() {
        return awaitOrCancel(asyncCommands.clusterFlushslots());
    }

    @Override
    public Long del(K... objects) {
        return awaitOrCancel(asyncCommands.del(objects));
    }

    @Override
    public List<KeyValue<K, V>> mget(K... objects) {
        return awaitOrCancel(asyncCommands.mget(objects));
    }

    @Override
    public String mset(Map<K, V> map) {
        return awaitOrCancel(asyncCommands.mset(map));
    }

    @Override
    public Boolean msetnx(Map<K, V> map) {
        return awaitOrCancel(asyncCommands.msetnx(map));
    }

    @Override
    public Long geoadd(K o, double v, double v1, V v2) {
        return awaitOrCancel(asyncCommands.geoadd(o, v, v1, v2));
    }

    @Override
    public Long geoadd(K o, Object... objects) {
        return awaitOrCancel(asyncCommands.geoadd(o, objects));
    }

    @Override
    public List<Value<String>> geohash(K o, V... objects) {
        return awaitOrCancel(asyncCommands.geohash(o, objects));
    }

    @Override
    public Set<V> georadius(K o, double v, double v1, double v2, GeoArgs.Unit unit) {
        return awaitOrCancel(asyncCommands.georadius(o, v, v1, v2, unit));
    }

    @Override
    public List<GeoWithin<V>> georadius(K o, double v, double v1, double v2, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return awaitOrCancel(asyncCommands.georadius(o, v, v1, v2, unit, geoArgs));
    }

    @Override
    public Long georadius(K o, double v, double v1, double v2, GeoArgs.Unit unit, GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return awaitOrCancel(asyncCommands.georadius(o, v, v1, v2, unit, geoRadiusStoreArgs));
    }

    @Override
    public Set<V> georadiusbymember(K o, V o2, double v1, GeoArgs.Unit unit) {
        return awaitOrCancel(asyncCommands.georadiusbymember(o, o2, v1, unit));
    }

    @Override
    public List<GeoWithin<V>> georadiusbymember(K o, V o2, double v1, GeoArgs.Unit unit, GeoArgs geoArgs) {
        return awaitOrCancel(asyncCommands.georadiusbymember(o, o2, v1, unit, geoArgs));
    }

    @Override
    public Long georadiusbymember(K o, V o2, double v1, GeoArgs.Unit unit, GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {
        return awaitOrCancel(asyncCommands.georadiusbymember(o, o2, v1, unit, geoRadiusStoreArgs));
    }

    @Override
    public List<GeoCoordinates> geopos(K o, V... objects) {
        return awaitOrCancel(asyncCommands.geopos(o, objects));
    }

    @Override
    public Double geodist(K o, V o2, V v1, GeoArgs.Unit unit) {
        return awaitOrCancel(asyncCommands.geodist(o, o2, v1, unit));
    }

    @Override
    public Long hdel(K o, K... objects) {
        return awaitOrCancel(asyncCommands.hdel(o, objects));
    }

    @Override
    public Boolean hexists(K o, K k1) {
        return awaitOrCancel(asyncCommands.hexists(o, k1));
    }

    @Override
    public V hget(K o, K k1) {
        return awaitOrCancel(asyncCommands.hget(o, k1));
    }

    @Override
    public Long hincrby(K o, K k1, long l) {
        return awaitOrCancel(asyncCommands.hincrby(o, k1, l));
    }

    @Override
    public Double hincrbyfloat(K o, K k1, double v) {
        return awaitOrCancel(asyncCommands.hincrbyfloat(o, k1, v));
    }

    @Override
    public Map<K, V> hgetall(K o) {
        return awaitOrCancel(asyncCommands.hgetall(o));
    }

    @Override
    public Long hgetall(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.hgetall(keyValueStreamingChannel, o));
    }

    @Override
    public List<K> hkeys(K o) {
        return awaitOrCancel(asyncCommands.hkeys(o));
    }

    @Override
    public Long hkeys(KeyStreamingChannel<K> keyStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.hkeys(keyStreamingChannel, o));
    }

    @Override
    public Long hlen(K o) {
        return awaitOrCancel(asyncCommands.hlen(o));
    }

    @Override
    public List<KeyValue<K, V>> hmget(K o, K... objects) {
        return awaitOrCancel(asyncCommands.hmget(o, objects));
    }

    @Override
    public Long hmget(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o, K... objects) {
        return awaitOrCancel(asyncCommands.hmget(keyValueStreamingChannel, o, objects));
    }

    @Override
    public String hmset(K o, Map<K, V> map) {
        return awaitOrCancel(asyncCommands.hmset(o, map));
    }

    @Override
    public MapScanCursor<K, V> hscan(K o) {
        return awaitOrCancel(asyncCommands.hscan(o));
    }

    @Override
    public MapScanCursor<K, V> hscan(K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.hscan(o, scanArgs));
    }

    @Override
    public MapScanCursor<K, V> hscan(K o, ScanCursor scanCursor, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.hscan(o, scanCursor, scanArgs));
    }

    @Override
    public MapScanCursor<K, V> hscan(K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.hscan(o, scanCursor));
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.hscan(keyValueStreamingChannel, o));
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.hscan(keyValueStreamingChannel, o, scanArgs));
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.hscan(keyValueStreamingChannel, o, scanCursor, scanArgs));
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.hscan(keyValueStreamingChannel, o, scanCursor));
    }

    @Override
    public Boolean hset(K o, K k1, V o2) {
        return awaitOrCancel(asyncCommands.hset(o, k1, o2));
    }

    @Override
    public Boolean hsetnx(K o, K k1, V o2) {
        return awaitOrCancel(asyncCommands.hsetnx(o, k1, o2));
    }

    @Override
    public Long hstrlen(K o, K k1) {
        return awaitOrCancel(asyncCommands.hstrlen(o, k1));
    }

    @Override
    public List<V> hvals(K o) {
        return awaitOrCancel(asyncCommands.hvals(o));
    }

    @Override
    public Long hvals(ValueStreamingChannel<V> valueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.hvals(valueStreamingChannel, o));
    }

    @Override
    public Long pfadd(K o, V... objects) {
        return awaitOrCancel(asyncCommands.pfadd(o, objects));
    }

    @Override
    public String pfmerge(K o, K... objects) {
        return awaitOrCancel(asyncCommands.pfmerge(o, objects));
    }

    @Override
    public Long pfcount(K... objects) {
        return awaitOrCancel(asyncCommands.pfcount(objects));
    }

    @Override
    public Long unlink(K... objects) {
        return awaitOrCancel(asyncCommands.unlink(objects));
    }

    @Override
    public byte[] dump(K o) {
        return awaitOrCancel(asyncCommands.dump(o));
    }

    @Override
    public Long exists(K... objects) {
        return awaitOrCancel(asyncCommands.exists(objects));
    }

    @Override
    public Boolean expire(K o, long l) {
        return awaitOrCancel(asyncCommands.expire(o, l));
    }

    @Override
    public Boolean expireat(K o, Date date) {
        return awaitOrCancel(asyncCommands.expireat(o, date));
    }

    @Override
    public Boolean expireat(K o, long l) {
        return awaitOrCancel(asyncCommands.expireat(o, l));
    }

    @Override
    public List<K> keys(K o) {
        return awaitOrCancel(asyncCommands.keys(o));
    }

    @Override
    public Long keys(KeyStreamingChannel<K> keyStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.keys(keyStreamingChannel, o));
    }

    @Override
    public String migrate(String s, int i, K o, int i1, long l) {
        return awaitOrCancel(asyncCommands.migrate(s, i, o, i1, l));
    }

    @Override
    public String migrate(String s, int i, int i1, long l, MigrateArgs<K> migrateArgs) {
        return awaitOrCancel(asyncCommands.migrate(s, i, i1, l, migrateArgs));
    }

    @Override
    public Boolean move(K o, int i) {
        return awaitOrCancel(asyncCommands.move(o, i));
    }

    @Override
    public String objectEncoding(K o) {
        return awaitOrCancel(asyncCommands.objectEncoding(o));
    }

    @Override
    public Long objectIdletime(K o) {
        return awaitOrCancel(asyncCommands.objectIdletime(o));
    }

    @Override
    public Long objectRefcount(K o) {
        return awaitOrCancel(asyncCommands.objectRefcount(o));
    }

    @Override
    public Boolean persist(K o) {
        return awaitOrCancel(asyncCommands.persist(o));
    }

    @Override
    public Boolean pexpire(K o, long l) {
        return awaitOrCancel(asyncCommands.pexpire(o, l));
    }

    @Override
    public Boolean pexpireat(K o, Date date) {
        return awaitOrCancel(asyncCommands.pexpireat(o, date));
    }

    @Override
    public Boolean pexpireat(K o, long l) {
        return awaitOrCancel(asyncCommands.pexpireat(o, l));
    }

    @Override
    public Long pttl(K o) {
        return awaitOrCancel(asyncCommands.pttl(o));
    }

    @Override
    public V randomkey() {
        return awaitOrCancel(asyncCommands.randomkey());
    }

    @Override
    public String rename(K o, K k1) {
        return awaitOrCancel(asyncCommands.rename(o, k1));
    }

    @Override
    public Boolean renamenx(K o, K k1) {
        return awaitOrCancel(asyncCommands.renamenx(o, k1));
    }

    @Override
    public String restore(K o, long l, byte[] bytes) {
        return awaitOrCancel(asyncCommands.restore(o, l, bytes));
    }

    @Override
    public String restore(K o, byte[] bytes, RestoreArgs restoreArgs) {
        return awaitOrCancel(asyncCommands.restore(o, bytes, restoreArgs));
    }

    @Override
    public List<V> sort(K o) {
        return awaitOrCancel(asyncCommands.sort(o));
    }

    @Override
    public Long sort(ValueStreamingChannel<V> valueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.sort(valueStreamingChannel, o));
    }

    @Override
    public List<V> sort(K o, SortArgs sortArgs) {
        return awaitOrCancel(asyncCommands.sort(o, sortArgs));
    }

    @Override
    public Long sort(ValueStreamingChannel<V> valueStreamingChannel, K o, SortArgs sortArgs) {
        return awaitOrCancel(asyncCommands.sort(valueStreamingChannel, o, sortArgs));
    }

    @Override
    public Long sortStore(K o, SortArgs sortArgs, K k1) {
        return awaitOrCancel(asyncCommands.sortStore(o, sortArgs, k1));
    }

    @Override
    public Long touch(K... objects) {
        return awaitOrCancel(asyncCommands.touch(objects));
    }

    @Override
    public Long ttl(K o) {
        return awaitOrCancel(asyncCommands.ttl(o));
    }

    @Override
    public String type(K o) {
        return awaitOrCancel(asyncCommands.type(o));
    }

    @Override
    public KeyScanCursor<K> scan() {
        return awaitOrCancel(asyncCommands.scan());
    }

    @Override
    public KeyScanCursor<K> scan(ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.scan(scanArgs));
    }

    @Override
    public KeyScanCursor<K> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.scan(scanCursor, scanArgs));
    }

    @Override
    public KeyScanCursor<K> scan(ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.scan(scanCursor));
    }

    @Override
    public StreamScanCursor scan(KeyStreamingChannel<K> keyStreamingChannel) {
        return awaitOrCancel(asyncCommands.scan(keyStreamingChannel));
    }

    @Override
    public StreamScanCursor scan(KeyStreamingChannel<K> keyStreamingChannel, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.scan(keyStreamingChannel, scanArgs));
    }

    @Override
    public StreamScanCursor scan(KeyStreamingChannel<K> keyStreamingChannel, ScanCursor scanCursor, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.scan(keyStreamingChannel, scanCursor, scanArgs));
    }

    @Override
    public StreamScanCursor scan(KeyStreamingChannel<K> keyStreamingChannel, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.scan(keyStreamingChannel, scanCursor));
    }

    @Override
    public KeyValue<K, V> blpop(long l, K... objects) {
        return awaitOrCancel(asyncCommands.blpop(l, objects));
    }

    @Override
    public KeyValue<K, V> brpop(long l, K... objects) {
        return awaitOrCancel(asyncCommands.brpop(l, objects));
    }

    @Override
    public V brpoplpush(long l, K o, K k1) {
        return awaitOrCancel(asyncCommands.brpoplpush(l, o, k1));
    }

    @Override
    public V lindex(K o, long l) {
        return awaitOrCancel(asyncCommands.lindex(o, l));
    }

    @Override
    public Long linsert(K o, boolean b, V o2, V v1) {
        return awaitOrCancel(asyncCommands.linsert(o, b, o2, v1));
    }

    @Override
    public Long llen(K o) {
        return awaitOrCancel(asyncCommands.llen(o));
    }

    @Override
    public V lpop(K o) {
        return awaitOrCancel(asyncCommands.lpop(o));
    }

    @Override
    public Long lpush(K o, V... objects) {
        return awaitOrCancel(asyncCommands.lpush(o, objects));
    }

    @Override
    public Long lpushx(K o, V... objects) {
        return awaitOrCancel(asyncCommands.lpushx(o, objects));
    }

    @Override
    public List<V> lrange(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.lrange(o, l, l1));
    }

    @Override
    public Long lrange(ValueStreamingChannel<V> valueStreamingChannel, K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.lrange(valueStreamingChannel, o, l, l1));
    }

    @Override
    public Long lrem(K o, long l, V o2) {
        return awaitOrCancel(asyncCommands.lrem(o, l, o2));
    }

    @Override
    public String lset(K o, long l, V o2) {
        return awaitOrCancel(asyncCommands.lset(o, l, o2));
    }

    @Override
    public String ltrim(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.ltrim(o, l, l1));
    }

    @Override
    public V rpop(K o) {
        return awaitOrCancel(asyncCommands.rpop(o));
    }

    @Override
    public V rpoplpush(K o, K k1) {
        return awaitOrCancel(asyncCommands.rpoplpush(o, k1));
    }

    @Override
    public Long rpush(K o, V... objects) {
        return awaitOrCancel(asyncCommands.rpush(o, objects));
    }

    @Override
    public Long rpushx(K o, V... objects) {
        return awaitOrCancel(asyncCommands.rpushx(o, objects));
    }

    @Override
    public <T> T eval(String s, ScriptOutputType scriptOutputType, K... objects) {
        return awaitOrCancel(asyncCommands.eval(s, scriptOutputType, objects));
    }

    @Override
    public <T> T eval(String s, ScriptOutputType scriptOutputType, K[] objects, V... objects2) {
        return awaitOrCancel(asyncCommands.eval(s, scriptOutputType, objects, objects2));
    }

    @Override
    public <T> T evalsha(String s, ScriptOutputType scriptOutputType, K... objects) {
        return awaitOrCancel(asyncCommands.evalsha(s, scriptOutputType, objects));
    }

    @Override
    public <T> T evalsha(String s, ScriptOutputType scriptOutputType, K[] objects, V... objects2) {
        return awaitOrCancel(asyncCommands.evalsha(s, scriptOutputType, objects, objects2));
    }

    @Override
    public List<Boolean> scriptExists(String... strings) {
        return awaitOrCancel(asyncCommands.scriptExists(strings));
    }

    @Override
    public String scriptFlush() {
        return awaitOrCancel(asyncCommands.scriptFlush());
    }

    @Override
    public String scriptKill() {
        return awaitOrCancel(asyncCommands.scriptKill());
    }

    @Override
    public String scriptLoad(V o) {
        return awaitOrCancel(asyncCommands.scriptLoad(o));
    }

    @Override
    public String digest(V o) {
        return asyncCommands.digest(o);
    }

    @Override
    public String bgrewriteaof() {
        return awaitOrCancel(asyncCommands.bgrewriteaof());
    }

    @Override
    public String bgsave() {
        return awaitOrCancel(asyncCommands.bgsave());
    }

    @Override
    public K clientGetname() {
        return awaitOrCancel(asyncCommands.clientGetname());
    }

    @Override
    public String clientSetname(K o) {
        return awaitOrCancel(asyncCommands.clientSetname(o));
    }

    @Override
    public String clientKill(String s) {
        return awaitOrCancel(asyncCommands.clientKill(s));
    }

    @Override
    public Long clientKill(KillArgs killArgs) {
        return awaitOrCancel(asyncCommands.clientKill(killArgs));
    }

    @Override
    public Long clientUnblock(long l, UnblockType unblockType) {
        return awaitOrCancel(asyncCommands.clientUnblock(l, unblockType));
    }

    @Override
    public String clientPause(long l) {
        return awaitOrCancel(asyncCommands.clientPause(l));
    }

    @Override
    public String clientList() {
        return awaitOrCancel(asyncCommands.clientList());
    }

    @Override
    public List<Object> command() {
        return awaitOrCancel(asyncCommands.command());
    }

    @Override
    public List<Object> commandInfo(String... strings) {
        return awaitOrCancel(asyncCommands.commandInfo(strings));
    }

    @Override
    public List<Object> commandInfo(CommandType... commandTypes) {
        return awaitOrCancel(asyncCommands.commandInfo(commandTypes));
    }

    @Override
    public Long commandCount() {
        return awaitOrCancel(asyncCommands.commandCount());
    }

    @Override
    public Map<String, String> configGet(String s) {
        return awaitOrCancel(asyncCommands.configGet(s));
    }

    @Override
    public String configResetstat() {
        return awaitOrCancel(asyncCommands.configResetstat());
    }

    @Override
    public String configRewrite() {
        return awaitOrCancel(asyncCommands.configRewrite());
    }

    @Override
    public String configSet(String s, String s1) {
        return awaitOrCancel(asyncCommands.configSet(s, s1));
    }

    @Override
    public Long dbsize() {
        return awaitOrCancel(asyncCommands.dbsize());
    }

    @Override
    public String debugCrashAndRecover(Long aLong) {
        return awaitOrCancel(asyncCommands.debugCrashAndRecover(aLong));
    }

    @Override
    public String debugHtstats(int i) {
        return awaitOrCancel(asyncCommands.debugHtstats(i));
    }

    @Override
    public String debugObject(K o) {
        return awaitOrCancel(asyncCommands.debugObject(o));
    }

    @Override
    public void debugOom() {
        asyncCommands.debugOom();
    }

    @Override
    public void debugSegfault() {
        asyncCommands.debugSegfault();
    }

    @Override
    public String debugReload() {
        return awaitOrCancel(asyncCommands.debugReload());
    }

    @Override
    public String debugRestart(Long aLong) {
        return awaitOrCancel(asyncCommands.debugRestart(aLong));
    }

    @Override
    public String debugSdslen(K o) {
        return awaitOrCancel(asyncCommands.debugSdslen(o));
    }

    @Override
    public String flushall() {
        return awaitOrCancel(asyncCommands.flushall());
    }

    @Override
    public String flushallAsync() {
        return awaitOrCancel(asyncCommands.flushallAsync());
    }

    @Override
    public String flushdb() {
        return awaitOrCancel(asyncCommands.flushdb());
    }

    @Override
    public String flushdbAsync() {
        return awaitOrCancel(asyncCommands.flushdbAsync());
    }

    @Override
    public String info() {
        return awaitOrCancel(asyncCommands.info());
    }

    @Override
    public String info(String s) {
        return awaitOrCancel(asyncCommands.info(s));
    }

    @Override
    public Date lastsave() {
        return awaitOrCancel(asyncCommands.lastsave());
    }

    @Override
    public Long memoryUsage(K key) {
        return awaitOrCancel(asyncCommands.memoryUsage(key));
    }

    @Override
    public String save() {
        return awaitOrCancel(asyncCommands.save());
    }

    @Override
    public void shutdown(boolean b) {
        asyncCommands.shutdown(b);
    }

    @Override
    public String slaveof(String s, int i) {
        return awaitOrCancel(asyncCommands.slaveof(s, i));
    }

    @Override
    public String slaveofNoOne() {
        return awaitOrCancel(asyncCommands.slaveofNoOne());
    }

    @Override
    public List<Object> slowlogGet() {
        return awaitOrCancel(asyncCommands.slowlogGet());
    }

    @Override
    public List<Object> slowlogGet(int i) {
        return awaitOrCancel(asyncCommands.slowlogGet(i));
    }

    @Override
    public Long slowlogLen() {
        return awaitOrCancel(asyncCommands.slowlogLen());
    }

    @Override
    public String slowlogReset() {
        return awaitOrCancel(asyncCommands.slowlogReset());
    }

    @Override
    public List<V> time() {
        return awaitOrCancel(asyncCommands.time());
    }

    @Override
    public Long sadd(K o, V... objects) {
        return awaitOrCancel(asyncCommands.sadd(o, objects));
    }

    @Override
    public Long scard(K o) {
        return awaitOrCancel(asyncCommands.scard(o));
    }

    @Override
    public Set<V> sdiff(K... objects) {
        return awaitOrCancel(asyncCommands.sdiff(objects));
    }

    @Override
    public Long sdiff(ValueStreamingChannel<V> valueStreamingChannel, K... objects) {
        return awaitOrCancel(asyncCommands.sdiff(valueStreamingChannel, objects));
    }

    @Override
    public Long sdiffstore(K o, K... objects) {
        return awaitOrCancel(asyncCommands.sdiffstore(o, objects));
    }

    @Override
    public Set<V> sinter(K... objects) {
        return awaitOrCancel(asyncCommands.sinter(objects));
    }

    @Override
    public Long sinter(ValueStreamingChannel<V> valueStreamingChannel, K... objects) {
        return awaitOrCancel(asyncCommands.sinter(valueStreamingChannel, objects));
    }

    @Override
    public Long sinterstore(K o, K... objects) {
        return awaitOrCancel(asyncCommands.sinterstore(o, objects));
    }

    @Override
    public Boolean sismember(K o, V o2) {
        return awaitOrCancel(asyncCommands.sismember(o, o2));
    }

    @Override
    public Boolean smove(K o, K k1, V o2) {
        return awaitOrCancel(asyncCommands.smove(o, k1, o2));
    }

    @Override
    public Set<V> smembers(K o) {
        return awaitOrCancel(asyncCommands.smembers(o));
    }

    @Override
    public Long smembers(ValueStreamingChannel<V> valueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.smembers(valueStreamingChannel, o));
    }

    @Override
    public V spop(K o) {
        return awaitOrCancel(asyncCommands.spop(o));
    }

    @Override
    public Set<V> spop(K o, long l) {
        return awaitOrCancel(asyncCommands.spop(o, l));
    }

    @Override
    public V srandmember(K o) {
        return awaitOrCancel(asyncCommands.srandmember(o));
    }

    @Override
    public List<V> srandmember(K o, long l) {
        return awaitOrCancel(asyncCommands.srandmember(o, l));
    }

    @Override
    public Long srandmember(ValueStreamingChannel<V> valueStreamingChannel, K o, long l) {
        return awaitOrCancel(asyncCommands.srandmember(valueStreamingChannel, o, l));
    }

    @Override
    public Long srem(K o, V... objects) {
        return awaitOrCancel(asyncCommands.srem(o, objects));
    }

    @Override
    public Set<V> sunion(K... objects) {
        return awaitOrCancel(asyncCommands.sunion(objects));
    }

    @Override
    public Long sunion(ValueStreamingChannel<V> valueStreamingChannel, K... objects) {
        return awaitOrCancel(asyncCommands.sunion(valueStreamingChannel, objects));
    }

    @Override
    public Long sunionstore(K o, K... objects) {
        return awaitOrCancel(asyncCommands.sunionstore(o, objects));
    }

    @Override
    public ValueScanCursor<V> sscan(K o) {
        return awaitOrCancel(asyncCommands.sscan(o));
    }

    @Override
    public ValueScanCursor<V> sscan(K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.sscan(o, scanArgs));
    }

    @Override
    public ValueScanCursor<V> sscan(K o, ScanCursor scanCursor, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.sscan(o, scanCursor, scanArgs));
    }

    @Override
    public ValueScanCursor<V> sscan(K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.sscan(o, scanCursor));
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<V> valueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.sscan(valueStreamingChannel, o));
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<V> valueStreamingChannel, K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.sscan(valueStreamingChannel, o, scanArgs));
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<V> valueStreamingChannel, K o, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.sscan(valueStreamingChannel, o, scanCursor, scanArgs));
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<V> valueStreamingChannel, K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.sscan(valueStreamingChannel, o, scanCursor));
    }

    @Override
    public KeyValue<K, ScoredValue<V>> bzpopmin(long l, K... objects) {
        return awaitOrCancel(asyncCommands.bzpopmin(l, objects));
    }

    @Override
    public KeyValue<K, ScoredValue<V>> bzpopmax(long l, K... objects) {
        return awaitOrCancel(asyncCommands.bzpopmax(l, objects));
    }

    @Override
    public Long zadd(K o, double v, V v1) {
        return awaitOrCancel(asyncCommands.zadd(o, v, v1));
    }

    @Override
    public Long zadd(K o, Object... objects) {
        return awaitOrCancel(asyncCommands.zadd(o, objects));
    }

    @Override
    public Long zadd(K o, ScoredValue<V>[] scoredValues) {
        return awaitOrCancel(asyncCommands.zadd(o, scoredValues));
    }

    @Override
    public Long zadd(K o, ZAddArgs zAddArgs, double v, V v1) {
        return awaitOrCancel(asyncCommands.zadd(o, zAddArgs, v, v1));
    }

    @Override
    public Long zadd(K o, ZAddArgs zAddArgs, Object... objects) {
        return awaitOrCancel(asyncCommands.zadd(o, zAddArgs, objects));
    }

    @Override
    public Long zadd(K o, ZAddArgs zAddArgs, ScoredValue<V>[] scoredValues) {
        return awaitOrCancel(asyncCommands.zadd(o, zAddArgs, scoredValues));
    }

    @Override
    public Double zaddincr(K o, double v, V v1) {
        return awaitOrCancel(asyncCommands.zaddincr(o, v, v1));
    }

    @Override
    public Double zaddincr(K o, ZAddArgs zAddArgs, double v, V v1) {
        return awaitOrCancel(asyncCommands.zaddincr(o, zAddArgs, v, v1));
    }

    @Override
    public Long zcard(K o) {
        return awaitOrCancel(asyncCommands.zcard(o));
    }

    @Override
    @Deprecated
    public Long zcount(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zcount(o, v, v1));
    }

    @Override
    @Deprecated
    public Long zcount(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zcount(o, s, s1));
    }

    @Override
    public Long zcount(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zcount(o, range));
    }

    @Override
    public Double zincrby(K o, double v, V v1) {
        return awaitOrCancel(asyncCommands.zincrby(o, v, v1));
    }

    @Override
    public Long zinterstore(K o, K... objects) {
        return awaitOrCancel(asyncCommands.zinterstore(o, objects));
    }

    @Override
    public Long zinterstore(K o, ZStoreArgs zStoreArgs, K... objects) {
        return awaitOrCancel(asyncCommands.zinterstore(o, zStoreArgs, objects));
    }

    @Override
    @Deprecated
    public Long zlexcount(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zlexcount(o, s, s1));
    }

    @Override
    public Long zlexcount(K o, Range<? extends V> range) {
        return awaitOrCancel(asyncCommands.zlexcount(o, range));
    }

    @Override
    public ScoredValue<V> zpopmin(K o) {
        return awaitOrCancel(asyncCommands.zpopmin(o));
    }

    @Override
    public List<ScoredValue<V>> zpopmin(K o, long l) {
        return awaitOrCancel(asyncCommands.zpopmin(o, l));
    }

    @Override
    public ScoredValue<V> zpopmax(K o) {
        return awaitOrCancel(asyncCommands.zpopmax(o));
    }

    @Override
    public List<ScoredValue<V>> zpopmax(K o, long l) {
        return awaitOrCancel(asyncCommands.zpopmax(o, l));
    }

    @Override
    public List<V> zrange(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrange(o, l, l1));
    }

    @Override
    public Long zrange(ValueStreamingChannel<V> valueStreamingChannel, K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrange(valueStreamingChannel, o, l, l1));
    }

    @Override
    public List<ScoredValue<V>> zrangeWithScores(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangeWithScores(o, l, l1));
    }

    @Override
    public Long zrangeWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangeWithScores(scoredValueStreamingChannel, o, l, l1));
    }

    @Override
    @Deprecated
    public List<V> zrangebylex(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrangebylex(o, s, s1));
    }

    @Override
    public List<V> zrangebylex(K o, Range<? extends V> range) {
        return awaitOrCancel(asyncCommands.zrangebylex(o, range));
    }

    @Override
    @Deprecated
    public List<V> zrangebylex(K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebylex(o, s, s1, l, l1));
    }

    @Override
    public List<V> zrangebylex(K o, Range<? extends V> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrangebylex(o, range, limit));
    }

    @Override
    @Deprecated
    public List<V> zrangebyscore(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, v, v1));
    }

    @Override
    @Deprecated
    public List<V> zrangebyscore(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, s, s1));
    }

    @Override
    public List<V> zrangebyscore(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, range));
    }

    @Override
    @Deprecated
    public List<V> zrangebyscore(K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public List<V> zrangebyscore(K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, s, s1, l, l1));
    }

    @Override
    public List<V> zrangebyscore(K o, Range<? extends Number> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrangebyscore(o, range, limit));
    }

    @Override
    @Deprecated
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, v, v1));
    }

    @Override
    @Deprecated
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, s, s1));
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, range));
    }

    @Override
    @Deprecated
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, s, s1, l, l1));
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, Range<? extends Number> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrangebyscore(valueStreamingChannel, o, range, limit));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, v, v1));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, s, s1));
    }

    @Override
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, range));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, s, s1, l, l1));
    }

    @Override
    public List<ScoredValue<V>> zrangebyscoreWithScores(K o, Range<? extends Number> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(o, range, limit));
    }

    @Override
    @Deprecated
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, double v,
            double v1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, v, v1));
    }

    @Override
    @Deprecated
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, String s,
            String s1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, s, s1));
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o,
            Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, range));
    }

    @Override
    @Deprecated
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, double v, double v1,
            long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, String s, String s1,
            long l, long l1) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, s, s1, l, l1));
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o,
            Range<? extends Number> range,
            Limit limit) {
        return awaitOrCancel(asyncCommands.zrangebyscoreWithScores(scoredValueStreamingChannel, o, range, limit));
    }

    @Override
    public Long zrank(K o, V o2) {
        return awaitOrCancel(asyncCommands.zrank(o, o2));
    }

    @Override
    public Long zrem(K o, V... objects) {
        return awaitOrCancel(asyncCommands.zrem(o, objects));
    }

    @Override
    @Deprecated
    public Long zremrangebylex(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zremrangebylex(o, s, s1));
    }

    @Override
    public Long zremrangebylex(K o, Range<? extends V> range) {
        return awaitOrCancel(asyncCommands.zremrangebylex(o, range));
    }

    @Override
    public Long zremrangebyrank(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zremrangebyrank(o, l, l1));
    }

    @Override
    @Deprecated
    public Long zremrangebyscore(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zremrangebyscore(o, v, v1));
    }

    @Override
    @Deprecated
    public Long zremrangebyscore(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zremrangebyscore(o, s, s1));
    }

    @Override
    public Long zremrangebyscore(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zremrangebyscore(o, range));
    }

    @Override
    public List<V> zrevrange(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrange(o, l, l1));
    }

    @Override
    public Long zrevrange(ValueStreamingChannel<V> valueStreamingChannel, K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrange(valueStreamingChannel, o, l, l1));
    }

    @Override
    public List<ScoredValue<V>> zrevrangeWithScores(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangeWithScores(o, l, l1));
    }

    @Override
    public Long zrevrangeWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangeWithScores(scoredValueStreamingChannel, o, l, l1));
    }

    @Override
    public List<V> zrevrangebylex(K o, Range<? extends V> range) {
        return awaitOrCancel(asyncCommands.zrevrangebylex(o, range));
    }

    @Override
    public List<V> zrevrangebylex(K o, Range<? extends V> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrevrangebylex(o, range, limit));
    }

    @Override
    @Deprecated
    public List<V> zrevrangebyscore(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, v, v1));
    }

    @Override
    @Deprecated
    public List<V> zrevrangebyscore(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, s, s1));
    }

    @Override
    public List<V> zrevrangebyscore(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, range));
    }

    @Override
    @Deprecated
    public List<V> zrevrangebyscore(K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public List<V> zrevrangebyscore(K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, s, s1, l, l1));
    }

    @Override
    public List<V> zrevrangebyscore(K o, Range<? extends Number> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(o, range, limit));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, v, v1));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, s, s1));
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, range));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, s, s1, l, l1));
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<V> valueStreamingChannel, K o, Range<? extends Number> range,
            Limit limit) {
        return awaitOrCancel(asyncCommands.zrevrangebyscore(valueStreamingChannel, o, range, limit));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, double v, double v1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, v, v1));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, String s, String s1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, s, s1));
    }

    @Override
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, range));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, double v, double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, String s, String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, s, s1, l, l1));
    }

    @Override
    public List<ScoredValue<V>> zrevrangebyscoreWithScores(K o, Range<? extends Number> range, Limit limit) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(o, range, limit));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, double v,
            double v1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, v, v1));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, String s,
            String s1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, s, s1));
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o,
            Range<? extends Number> range) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, range));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, double v,
            double v1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, v, v1, l, l1));
    }

    @Override
    @Deprecated
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, String s,
            String s1, long l, long l1) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, s, s1, l, l1));
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o,
            Range<? extends Number> range,
            Limit limit) {
        return awaitOrCancel(asyncCommands.zrevrangebyscoreWithScores(scoredValueStreamingChannel, o, range, limit));
    }

    @Override
    public Long zrevrank(K o, V o2) {
        return awaitOrCancel(asyncCommands.zrevrank(o, o2));
    }

    @Override
    public ScoredValueScanCursor<V> zscan(K o) {
        return awaitOrCancel(asyncCommands.zscan(o));
    }

    @Override
    public ScoredValueScanCursor<V> zscan(K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.zscan(o, scanArgs));
    }

    @Override
    public ScoredValueScanCursor<V> zscan(K o, ScanCursor scanCursor, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.zscan(o, scanCursor, scanArgs));
    }

    @Override
    public ScoredValueScanCursor<V> zscan(K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.zscan(o, scanCursor));
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o) {
        return awaitOrCancel(asyncCommands.zscan(scoredValueStreamingChannel, o));
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.zscan(scoredValueStreamingChannel, o, scanArgs));
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        return awaitOrCancel(asyncCommands.zscan(scoredValueStreamingChannel, o, scanCursor, scanArgs));
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<V> scoredValueStreamingChannel, K o, ScanCursor scanCursor) {
        return awaitOrCancel(asyncCommands.zscan(scoredValueStreamingChannel, o, scanCursor));
    }

    @Override
    public Double zscore(K o, V o2) {
        return awaitOrCancel(asyncCommands.zscore(o, o2));
    }

    @Override
    public Long zunionstore(K o, K... objects) {
        return awaitOrCancel(asyncCommands.zunionstore(o, objects));
    }

    @Override
    public Long zunionstore(K o, ZStoreArgs zStoreArgs, K... objects) {
        return awaitOrCancel(asyncCommands.zunionstore(o, zStoreArgs, objects));
    }

    @Override
    public Long xack(K o, K k1, String... strings) {
        return awaitOrCancel(asyncCommands.xack(o, k1, strings));
    }

    @Override
    public String xadd(K o, Map<K, V> map) {
        return awaitOrCancel(asyncCommands.xadd(o, map));
    }

    @Override
    public String xadd(K o, XAddArgs xAddArgs, Map<K, V> map) {
        return awaitOrCancel(asyncCommands.xadd(o, xAddArgs, map));
    }

    @Override
    public String xadd(K o, Object... objects) {
        return awaitOrCancel(asyncCommands.xadd(o, objects));
    }

    @Override
    public String xadd(K o, XAddArgs xAddArgs, Object... objects) {
        return awaitOrCancel(asyncCommands.xadd(o, xAddArgs, objects));
    }

    @Override
    public List<StreamMessage<K, V>> xclaim(K o, Consumer<K> consumer, long l, String... strings) {
        return awaitOrCancel(asyncCommands.xclaim(o, consumer, l, strings));
    }

    @Override
    public List<StreamMessage<K, V>> xclaim(K o, Consumer<K> consumer, XClaimArgs xClaimArgs, String... strings) {
        return awaitOrCancel(asyncCommands.xclaim(o, consumer, xClaimArgs, strings));
    }

    @Override
    public Long xdel(K o, String... strings) {
        return awaitOrCancel(asyncCommands.xdel(o, strings));
    }

    @Override
    public String xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K o) {
        return awaitOrCancel(asyncCommands.xgroupCreate(streamOffset, o));
    }

    @Override
    public String xgroupCreate(XReadArgs.StreamOffset<K> streamOffset, K group, XGroupCreateArgs args) {
        return awaitOrCancel(asyncCommands.xgroupCreate(streamOffset, group, args));
    }

    @Override
    public Boolean xgroupDelconsumer(K o, Consumer<K> consumer) {
        return awaitOrCancel(asyncCommands.xgroupDelconsumer(o, consumer));
    }

    @Override
    public Boolean xgroupDestroy(K o, K k1) {
        return awaitOrCancel(asyncCommands.xgroupDestroy(o, k1));
    }

    @Override
    public String xgroupSetid(XReadArgs.StreamOffset<K> streamOffset, K o) {
        return awaitOrCancel(asyncCommands.xgroupSetid(streamOffset, o));
    }

    @Override
    public List<Object> xinfoStream(K key) {
        return awaitOrCancel(asyncCommands.xinfoStream(key));
    }

    @Override
    public List<Object> xinfoGroups(K key) {
        return awaitOrCancel(asyncCommands.xinfoGroups(key));
    }

    @Override
    public List<Object> xinfoConsumers(K key, K group) {
        return awaitOrCancel(asyncCommands.xinfoConsumers(key, group));
    }

    @Override
    public Long xlen(K o) {
        return awaitOrCancel(asyncCommands.xlen(o));
    }

    @Override
    public List<Object> xpending(K o, K k1) {
        return awaitOrCancel(asyncCommands.xpending(o, k1));
    }

    @Override
    public List<Object> xpending(K o, K k1, Range<String> range, Limit limit) {
        return awaitOrCancel(asyncCommands.xpending(o, k1, range, limit));
    }

    @Override
    public List<Object> xpending(K o, Consumer<K> consumer, Range<String> range, Limit limit) {
        return awaitOrCancel(asyncCommands.xpending(o, consumer, range, limit));
    }

    @Override
    public List<StreamMessage<K, V>> xrange(K o, Range<String> range) {
        return awaitOrCancel(asyncCommands.xrange(o, range));
    }

    @Override
    public List<StreamMessage<K, V>> xrange(K o, Range<String> range, Limit limit) {
        return awaitOrCancel(asyncCommands.xrange(o, range, limit));
    }

    @Override
    public List<StreamMessage<K, V>> xread(XReadArgs.StreamOffset<K>[] streamOffsets) {
        return awaitOrCancel(asyncCommands.xread(streamOffsets));
    }

    @Override
    public List<StreamMessage<K, V>> xread(XReadArgs xReadArgs, XReadArgs.StreamOffset<K>[] streamOffsets) {
        return awaitOrCancel(asyncCommands.xread(xReadArgs, streamOffsets));
    }

    @Override
    public List<StreamMessage<K, V>> xreadgroup(Consumer<K> consumer, XReadArgs.StreamOffset<K>[] streamOffsets) {
        return awaitOrCancel(asyncCommands.xreadgroup(consumer, streamOffsets));
    }

    @Override
    public List<StreamMessage<K, V>> xreadgroup(Consumer<K> consumer, XReadArgs xReadArgs,
            XReadArgs.StreamOffset<K>[] streamOffsets) {
        return awaitOrCancel(asyncCommands.xreadgroup(consumer, xReadArgs, streamOffsets));
    }

    @Override
    public List<StreamMessage<K, V>> xrevrange(K o, Range<String> range) {
        return awaitOrCancel(asyncCommands.xrevrange(o, range));
    }

    @Override
    public List<StreamMessage<K, V>> xrevrange(K o, Range<String> range, Limit limit) {
        return awaitOrCancel(asyncCommands.xrevrange(o, range, limit));
    }

    @Override
    public Long xtrim(K o, long l) {
        return awaitOrCancel(asyncCommands.xtrim(o, l));
    }

    @Override
    public Long xtrim(K o, boolean b, long l) {
        return awaitOrCancel(asyncCommands.xtrim(o, b, l));
    }

    @Override
    public Long append(K o, V o2) {
        return awaitOrCancel(asyncCommands.append(o, o2));
    }

    @Override
    public Long bitcount(K o) {
        return awaitOrCancel(asyncCommands.bitcount(o));
    }

    @Override
    public Long bitcount(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.bitcount(o, l, l1));
    }

    @Override
    public List<Long> bitfield(K o, BitFieldArgs bitFieldArgs) {
        return awaitOrCancel(asyncCommands.bitfield(o, bitFieldArgs));
    }

    @Override
    public Long bitpos(K o, boolean b) {
        return awaitOrCancel(asyncCommands.bitpos(o, b));
    }

    @Override
    public Long bitpos(K o, boolean b, long l) {
        return awaitOrCancel(asyncCommands.bitpos(o, b, l));
    }

    @Override
    public Long bitpos(K o, boolean b, long l, long l1) {
        return awaitOrCancel(asyncCommands.bitpos(o, b, l, l1));
    }

    @Override
    public Long bitopAnd(K o, K... objects) {
        return awaitOrCancel(asyncCommands.bitopAnd(o, objects));
    }

    @Override
    public Long bitopNot(K o, K k1) {
        return awaitOrCancel(asyncCommands.bitopNot(o, k1));
    }

    @Override
    public Long bitopOr(K o, K... objects) {
        return awaitOrCancel(asyncCommands.bitopOr(o, objects));
    }

    @Override
    public Long bitopXor(K o, K... objects) {
        return awaitOrCancel(asyncCommands.bitopXor(o, objects));
    }

    @Override
    public Long decr(K o) {
        return awaitOrCancel(asyncCommands.decr(o));
    }

    @Override
    public Long decrby(K o, long l) {
        return awaitOrCancel(asyncCommands.decrby(o, l));
    }

    @Override
    public V get(K o) {
        return awaitOrCancel(asyncCommands.get(o));
    }

    @Override
    public Long getbit(K o, long l) {
        return awaitOrCancel(asyncCommands.getbit(o, l));
    }

    @Override
    public V getrange(K o, long l, long l1) {
        return awaitOrCancel(asyncCommands.getrange(o, l, l1));
    }

    @Override
    public V getset(K o, V o2) {
        return awaitOrCancel(asyncCommands.getset(o, o2));
    }

    @Override
    public Long incr(K o) {
        return awaitOrCancel(asyncCommands.incr(o));
    }

    @Override
    public Long incrby(K o, long l) {
        return awaitOrCancel(asyncCommands.incrby(o, l));
    }

    @Override
    public Double incrbyfloat(K o, double v) {
        return awaitOrCancel(asyncCommands.incrbyfloat(o, v));
    }

    @Override
    public Long mget(KeyValueStreamingChannel<K, V> keyValueStreamingChannel, K... objects) {
        return awaitOrCancel(asyncCommands.mget(keyValueStreamingChannel, objects));
    }

    @Override
    public String set(K o, V o2) {
        return awaitOrCancel(asyncCommands.set(o, o2));
    }

    @Override
    public String set(K o, V o2, SetArgs setArgs) {
        return awaitOrCancel(asyncCommands.set(o, o2, setArgs));
    }

    @Override
    public Long setbit(K o, long l, int i) {
        return awaitOrCancel(asyncCommands.setbit(o, l, i));
    }

    @Override
    public String setex(K o, long l, V o2) {
        return awaitOrCancel(asyncCommands.setex(o, l, o2));
    }

    @Override
    public String psetex(K o, long l, V o2) {
        return awaitOrCancel(asyncCommands.psetex(o, l, o2));
    }

    @Override
    public Boolean setnx(K o, V o2) {
        return awaitOrCancel(asyncCommands.setnx(o, o2));
    }

    @Override
    public Long setrange(K o, long l, V o2) {
        return awaitOrCancel(asyncCommands.setrange(o, l, o2));
    }

    @Override
    public Long strlen(K o) {
        return awaitOrCancel(asyncCommands.strlen(o));
    }

    @Override
    public String discard() {
        return awaitOrCancel(asyncCommands.discard());
    }

    @Override
    public TransactionResult exec() {
        return awaitOrCancel(asyncCommands.exec());
    }

    @Override
    public String multi() {
        return awaitOrCancel(asyncCommands.multi());
    }

    @Override
    public String watch(K... objects) {
        return awaitOrCancel(asyncCommands.watch(objects));
    }

    @Override
    public String unwatch() {
        return awaitOrCancel(asyncCommands.unwatch());
    }

    @Override
    public String select(int i) {
        return asyncCommands.select(i);
    }

    @Override
    public String swapdb(int i, int i1) {
        return awaitOrCancel(asyncCommands.swapdb(i, i1));
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return asyncCommands.getStatefulConnection();
    }

    @Override
    public Long publish(K o, V o2) {
        return awaitOrCancel(asyncCommands.publish(o, o2));
    }

    @Override
    public List<K> pubsubChannels() {
        return awaitOrCancel(asyncCommands.pubsubChannels());
    }

    @Override
    public List<K> pubsubChannels(K o) {
        return awaitOrCancel(asyncCommands.pubsubChannels(o));
    }

    @Override
    public Map<K, Long> pubsubNumsub(K... objects) {
        return awaitOrCancel(asyncCommands.pubsubNumsub(objects));
    }

    @Override
    public Long pubsubNumpat() {
        return awaitOrCancel(asyncCommands.pubsubNumpat());
    }

    @Override
    public V echo(V o) {
        return awaitOrCancel(asyncCommands.echo(o));
    }

    @Override
    public List<Object> role() {
        return awaitOrCancel(asyncCommands.role());
    }

    @Override
    public String ping() {
        return awaitOrCancel(asyncCommands.ping());
    }

    @Override
    public String readOnly() {
        return awaitOrCancel(asyncCommands.readOnly());
    }

    @Override
    public String readWrite() {
        return awaitOrCancel(asyncCommands.readWrite());
    }

    @Override
    public String quit() {
        return awaitOrCancel(asyncCommands.quit());
    }

    @Override
    public Long waitForReplication(int i, long l) {
        return awaitOrCancel(asyncCommands.waitForReplication(i, l));
    }

    @Override
    public <T> T dispatch(ProtocolKeyword protocolKeyword, CommandOutput<K, V, T> commandOutput,
            CommandArgs<K, V> commandArgs) {
        return awaitOrCancel(asyncCommands.dispatch(protocolKeyword, commandOutput, commandArgs));
    }

    @Override
    public <T> T dispatch(ProtocolKeyword protocolKeyword, CommandOutput<K, V, T> commandOutput) {
        return awaitOrCancel(asyncCommands.dispatch(protocolKeyword, commandOutput));
    }

    @Override
    public boolean isOpen() {
        return asyncCommands.isOpen();
    }

    @Override
    public void reset() {
        asyncCommands.reset();
    }

    private <T> T awaitOrCancel(RedisFuture<T> future) {
        return LettuceFutures.awaitOrCancel(future, getTimeout(future), TimeUnit.NANOSECONDS);
    }

    private long getTimeout(RedisFuture<?> future) {
        if (future instanceof RedisCommand) {
            return this.timeoutProvider.getTimeoutNs((RedisCommand) future);
        }

        return this.getStatefulConnection().getTimeout().getNano();
    }
}
