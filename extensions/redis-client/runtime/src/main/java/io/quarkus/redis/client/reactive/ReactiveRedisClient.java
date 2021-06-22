package io.quarkus.redis.client.reactive;

import static io.quarkus.redis.client.runtime.RedisClientUtil.DEFAULT_CLIENT;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.runtime.RedisClientsProducer;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

/**
 * A Redis client offering reactive Redis commands.
 *
 * For more information about how each individual command visit
 * the <a href="https://redis.io/commands">Redis Commands Page</a>
 */
public interface ReactiveRedisClient {
    /**
     * Creates the {@link RedisClient} using the default redis client configuration
     * 
     * @return {@link ReactiveRedisClient} - the default reactive redis client
     */
    static ReactiveRedisClient createClient() {
        return createClient(DEFAULT_CLIENT);
    }

    /**
     * Creates the {@link RedisClient} using the named redis client configuration
     * 
     * @return {@link ReactiveRedisClient} - the named reactive redis client
     */
    static ReactiveRedisClient createClient(String name) {
        RedisClientsProducer redisClientsProducer = Arc.container().instance(RedisClientsProducer.class).get();
        return redisClientsProducer.getReactiveRedisClient(name);
    }

    void close();

    Uni<Response> append(String arg0, String arg1);

    Response appendAndAwait(String arg0, String arg1);

    Uni<Response> asking();

    Response askingAndAwait();

    Uni<Response> auth(List<String> args);

    Response authAndAwait(List<String> args);

    Uni<Response> bgrewriteaof();

    Response bgrewriteaofAndAwait();

    Uni<Response> bgsave(List<String> args);

    Response bgsaveAndAwait(List<String> args);

    Uni<Response> bitcount(List<String> args);

    Response bitcountAndAwait(List<String> args);

    Uni<Response> bitfield(List<String> args);

    Response bitfieldAndAwait(List<String> args);

    Uni<Response> bitop(List<String> args);

    Response bitopAndAwait(List<String> args);

    Uni<Response> bitpos(List<String> args);

    Response bitposAndAwait(List<String> args);

    Uni<Response> blpop(List<String> args);

    Response blpopAndAwait(List<String> args);

    Uni<Response> brpop(List<String> args);

    Response brpopAndAwait(List<String> args);

    Uni<Response> brpoplpush(String arg0, String arg1, String arg2);

    Response brpoplpushAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> bzpopmax(List<String> args);

    Response bzpopmaxAndAwait(List<String> args);

    Uni<Response> bzpopmin(List<String> args);

    Response bzpopminAndAwait(List<String> args);

    Uni<Response> client(List<String> args);

    Response clientAndAwait(List<String> args);

    Uni<Response> cluster(List<String> args);

    Response clusterAndAwait(List<String> args);

    Uni<Response> command(List<String> args);

    Response commandAndAwait(List<String> args);

    Uni<Response> config(List<String> args);

    Response configAndAwait(List<String> args);

    Uni<Response> dbsize();

    Response dbsizeAndAwait();

    Uni<Response> debug(List<String> args);

    Response debugAndAwait(List<String> args);

    Uni<Response> decr(String arg0);

    Response decrAndAwait(String arg0);

    Uni<Response> decrby(String arg0, String arg1);

    Response decrbyAndAwait(String arg0, String arg1);

    Uni<Response> del(List<String> args);

    Response delAndAwait(List<String> args);

    Uni<Response> discard();

    Response discardAndAwait();

    Uni<Response> dump(String arg0);

    Response dumpAndAwait(String arg0);

    Uni<Response> echo(String arg0);

    Response echoAndAwait(String arg0);

    Uni<Response> eval(List<String> args);

    Response evalAndAwait(List<String> args);

    Uni<Response> evalsha(List<String> args);

    Response evalshaAndAwait(List<String> args);

    Uni<Response> exec();

    Response execAndAwait();

    Uni<Response> exists(List<String> args);

    Response existsAndAwait(List<String> args);

    Uni<Response> expire(String arg0, String arg1);

    Response expireAndAwait(String arg0, String arg1);

    Uni<Response> expireat(String arg0, String arg1);

    Response expireatAndAwait(String arg0, String arg1);

    Uni<Response> flushall(List<String> args);

    Response flushallAndAwait(List<String> args);

    Uni<Response> flushdb(List<String> args);

    Response flushdbAndAwait(List<String> args);

    Uni<Response> geoadd(List<String> args);

    Response geoaddAndAwait(List<String> args);

    Uni<Response> geodist(List<String> args);

    Response geodistAndAwait(List<String> args);

    Uni<Response> geohash(List<String> args);

    Response geohashAndAwait(List<String> args);

    Uni<Response> geopos(List<String> args);

    Response geoposAndAwait(List<String> args);

    Uni<Response> georadius(List<String> args);

    Response georadiusAndAwait(List<String> args);

    Uni<Response> georadiusRo(List<String> args);

    Response georadiusRoAndAwait(List<String> args);

    Uni<Response> georadiusbymember(List<String> args);

    Response georadiusbymemberAndAwait(List<String> args);

    Uni<Response> georadiusbymemberRo(List<String> args);

    Response georadiusbymemberRoAndAwait(List<String> args);

    Uni<Response> get(String arg0);

    Response getAndAwait(String arg0);

    Uni<Response> getbit(String arg0, String arg1);

    Response getbitAndAwait(String arg0, String arg1);

    Uni<Response> getrange(String arg0, String arg1, String arg2);

    Response getrangeAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> getset(String arg0, String arg1);

    Response getsetAndAwait(String arg0, String arg1);

    Uni<Response> hdel(List<String> args);

    Response hdelAndAwait(List<String> args);

    Uni<Response> hexists(String arg0, String arg1);

    Response hexistsAndAwait(String arg0, String arg1);

    Uni<Response> hget(String arg0, String arg1);

    Response hgetAndAwait(String arg0, String arg1);

    Uni<Response> hgetall(String arg0);

    Response hgetallAndAwait(String arg0);

    Uni<Response> hincrby(String arg0, String arg1, String arg2);

    Response hincrbyAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> hincrbyfloat(String arg0, String arg1, String arg2);

    Response hincrbyfloatAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> hkeys(String arg0);

    Response hkeysAndAwait(String arg0);

    Uni<Response> hlen(String arg0);

    Response hlenAndAwait(String arg0);

    Uni<Response> hmget(List<String> args);

    Response hmgetAndAwait(List<String> args);

    Uni<Response> hmset(List<String> args);

    Response hmsetAndAwait(List<String> args);

    Uni<Response> host(List<String> args);

    Response hostAndAwait(List<String> args);

    Uni<Response> hscan(List<String> args);

    Response hscanAndAwait(List<String> args);

    Uni<Response> hset(List<String> args);

    Response hsetAndAwait(List<String> args);

    Uni<Response> hsetnx(String arg0, String arg1, String arg2);

    Response hsetnxAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> hstrlen(String arg0, String arg1);

    Response hstrlenAndAwait(String arg0, String arg1);

    Uni<Response> hvals(String arg0);

    Response hvalsAndAwait(String arg0);

    Uni<Response> incr(String arg0);

    Response incrAndAwait(String arg0);

    Uni<Response> incrby(String arg0, String arg1);

    Response incrbyAndAwait(String arg0, String arg1);

    Uni<Response> incrbyfloat(String arg0, String arg1);

    Response incrbyfloatAndAwait(String arg0, String arg1);

    Uni<Response> info(List<String> args);

    Response infoAndAwait(List<String> args);

    Uni<Response> keys(String arg0);

    Response keysAndAwait(String arg0);

    Uni<Response> lastsave();

    Response lastsaveAndAwait();

    Uni<Response> latency(List<String> args);

    Response latencyAndAwait(List<String> args);

    Uni<Response> lindex(String arg0, String arg1);

    Response lindexAndAwait(String arg0, String arg1);

    Uni<Response> linsert(String arg0, String arg1, String arg2, String arg3);

    Response linsertAndAwait(String arg0, String arg1, String arg2, String arg3);

    Uni<Response> llen(String arg0);

    Response llenAndAwait(String arg0);

    Uni<Response> lolwut(List<String> args);

    Response lolwutAndAwait(List<String> args);

    Uni<Response> lpop(String arg0);

    Response lpopAndAwait(String arg0);

    Uni<Response> lpush(List<String> args);

    Response lpushAndAwait(List<String> args);

    Uni<Response> lpushx(List<String> args);

    Response lpushxAndAwait(List<String> args);

    Uni<Response> lrange(String arg0, String arg1, String arg2);

    Response lrangeAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> lrem(String arg0, String arg1, String arg2);

    Response lremAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> lset(String arg0, String arg1, String arg2);

    Response lsetAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> ltrim(String arg0, String arg1, String arg2);

    Response ltrimAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> memory(List<String> args);

    Response memoryAndAwait(List<String> args);

    Uni<Response> mget(List<String> args);

    Response mgetAndAwait(List<String> args);

    Uni<Response> migrate(List<String> args);

    Response migrateAndAwait(List<String> args);

    Uni<Response> module(List<String> args);

    Response moduleAndAwait(List<String> args);

    Uni<Response> monitor();

    Response monitorAndAwait();

    Uni<Response> move(String arg0, String arg1);

    Response moveAndAwait(String arg0, String arg1);

    Uni<Response> mset(List<String> args);

    Response msetAndAwait(List<String> args);

    Uni<Response> msetnx(List<String> args);

    Response msetnxAndAwait(List<String> args);

    Uni<Response> multi();

    Response multiAndAwait();

    Uni<Response> object(List<String> args);

    Response objectAndAwait(List<String> args);

    Uni<Response> persist(String arg0);

    Response persistAndAwait(String arg0);

    Uni<Response> pexpire(String arg0, String arg1);

    Response pexpireAndAwait(String arg0, String arg1);

    Uni<Response> pexpireat(String arg0, String arg1);

    Response pexpireatAndAwait(String arg0, String arg1);

    Uni<Response> pfadd(List<String> args);

    Response pfaddAndAwait(List<String> args);

    Uni<Response> pfcount(List<String> args);

    Response pfcountAndAwait(List<String> args);

    Uni<Response> pfdebug(List<String> args);

    Response pfdebugAndAwait(List<String> args);

    Uni<Response> pfmerge(List<String> args);

    Response pfmergeAndAwait(List<String> args);

    Uni<Response> pfselftest();

    Response pfselftestAndAwait();

    Uni<Response> ping(List<String> args);

    Response pingAndAwait(List<String> args);

    Uni<Response> post(List<String> args);

    Response postAndAwait(List<String> args);

    Uni<Response> psetex(String arg0, String arg1, String arg2);

    Response psetexAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> psubscribe(List<String> args);

    Response psubscribeAndAwait(List<String> args);

    Uni<Response> psync(String arg0, String arg1);

    Response psyncAndAwait(String arg0, String arg1);

    Uni<Response> pttl(String arg0);

    Response pttlAndAwait(String arg0);

    Uni<Response> publish(String arg0, String arg1);

    Response publishAndAwait(String arg0, String arg1);

    Uni<Response> pubsub(List<String> args);

    Response pubsubAndAwait(List<String> args);

    Uni<Response> punsubscribe(List<String> args);

    Response punsubscribeAndAwait(List<String> args);

    Uni<Response> randomkey();

    Response randomkeyAndAwait();

    Uni<Response> readonly();

    Response readonlyAndAwait();

    Uni<Response> readwrite();

    Response readwriteAndAwait();

    Uni<Response> rename(String arg0, String arg1);

    Response renameAndAwait(String arg0, String arg1);

    Uni<Response> renamenx(String arg0, String arg1);

    Response renamenxAndAwait(String arg0, String arg1);

    Uni<Response> replconf(List<String> args);

    Response replconfAndAwait(List<String> args);

    Uni<Response> replicaof(String arg0, String arg1);

    Response replicaofAndAwait(String arg0, String arg1);

    Uni<Response> restore(List<String> args);

    Response restoreAndAwait(List<String> args);

    Uni<Response> restoreAsking(List<String> args);

    Response restoreAskingAndAwait(List<String> args);

    Uni<Response> role();

    Response roleAndAwait();

    Uni<Response> rpop(String arg0);

    Response rpopAndAwait(String arg0);

    Uni<Response> rpoplpush(String arg0, String arg1);

    Response rpoplpushAndAwait(String arg0, String arg1);

    Uni<Response> rpush(List<String> args);

    Response rpushAndAwait(List<String> args);

    Uni<Response> rpushx(List<String> args);

    Response rpushxAndAwait(List<String> args);

    Uni<Response> sadd(List<String> args);

    Response saddAndAwait(List<String> args);

    Uni<Response> save();

    Response saveAndAwait();

    Uni<Response> scan(List<String> args);

    Response scanAndAwait(List<String> args);

    Uni<Response> scard(String arg0);

    Response scardAndAwait(String arg0);

    Uni<Response> script(List<String> args);

    Response scriptAndAwait(List<String> args);

    Uni<Response> sdiff(List<String> args);

    Response sdiffAndAwait(List<String> args);

    Uni<Response> sdiffstore(List<String> args);

    Response sdiffstoreAndAwait(List<String> args);

    Uni<Response> select(String arg0);

    Response selectAndAwait(String arg0);

    Uni<Response> set(List<String> args);

    Response setAndAwait(List<String> args);

    Uni<Response> setbit(String arg0, String arg1, String arg2);

    Response setbitAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> setex(String arg0, String arg1, String arg2);

    Response setexAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> setnx(String arg0, String arg1);

    Response setnxAndAwait(String arg0, String arg1);

    Uni<Response> setrange(String arg0, String arg1, String arg2);

    Response setrangeAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> shutdown(List<String> args);

    Response shutdownAndAwait(List<String> args);

    Uni<Response> sinter(List<String> args);

    Response sinterAndAwait(List<String> args);

    Uni<Response> sinterstore(List<String> args);

    Response sinterstoreAndAwait(List<String> args);

    Uni<Response> sismember(String arg0, String arg1);

    Response sismemberAndAwait(String arg0, String arg1);

    Uni<Response> slaveof(String arg0, String arg1);

    Response slaveofAndAwait(String arg0, String arg1);

    Uni<Response> slowlog(List<String> args);

    Response slowlogAndAwait(List<String> args);

    Uni<Response> smembers(String arg0);

    Response smembersAndAwait(String arg0);

    Uni<Response> smove(String arg0, String arg1, String arg2);

    Response smoveAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> sort(List<String> args);

    Response sortAndAwait(List<String> args);

    Uni<Response> spop(List<String> args);

    Response spopAndAwait(List<String> args);

    Uni<Response> srandmember(List<String> args);

    Response srandmemberAndAwait(List<String> args);

    Uni<Response> srem(List<String> args);

    Response sremAndAwait(List<String> args);

    Uni<Response> sscan(List<String> args);

    Response sscanAndAwait(List<String> args);

    Uni<Response> strlen(String arg0);

    Response strlenAndAwait(String arg0);

    Uni<Response> subscribe(List<String> args);

    Response subscribeAndAwait(List<String> args);

    Uni<Response> substr(String arg0, String arg1, String arg2);

    Response substrAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> sunion(List<String> args);

    Response sunionAndAwait(List<String> args);

    Uni<Response> sunionstore(List<String> args);

    Response sunionstoreAndAwait(List<String> args);

    Uni<Response> swapdb(String arg0, String arg1);

    Response swapdbAndAwait(String arg0, String arg1);

    Uni<Response> sync();

    Response syncAndAwait();

    Uni<Response> time();

    Response timeAndAwait();

    Uni<Response> touch(List<String> args);

    Response touchAndAwait(List<String> args);

    Uni<Response> ttl(String arg0);

    Response ttlAndAwait(String arg0);

    Uni<Response> type(String arg0);

    Response typeAndAwait(String arg0);

    Uni<Response> unlink(List<String> args);

    Response unlinkAndAwait(List<String> args);

    Uni<Response> unsubscribe(List<String> args);

    Response unsubscribeAndAwait(List<String> args);

    Uni<Response> unwatch();

    Response unwatchAndAwait();

    Uni<Response> wait(String arg0, String arg1);

    Response waitAndAwait(String arg0, String arg1);

    Uni<Response> watch(List<String> args);

    Response watchAndAwait(List<String> args);

    Uni<Response> xack(List<String> args);

    Response xackAndAwait(List<String> args);

    Uni<Response> xadd(List<String> args);

    Response xaddAndAwait(List<String> args);

    Uni<Response> xclaim(List<String> args);

    Response xclaimAndAwait(List<String> args);

    Uni<Response> xdel(List<String> args);

    Response xdelAndAwait(List<String> args);

    Uni<Response> xgroup(List<String> args);

    Response xgroupAndAwait(List<String> args);

    Uni<Response> xinfo(List<String> args);

    Response xinfoAndAwait(List<String> args);

    Uni<Response> xlen(String arg0);

    Response xlenAndAwait(String arg0);

    Uni<Response> xpending(List<String> args);

    Response xpendingAndAwait(List<String> args);

    Uni<Response> xrange(List<String> args);

    Response xrangeAndAwait(List<String> args);

    Uni<Response> xread(List<String> args);

    Response xreadAndAwait(List<String> args);

    Uni<Response> xreadgroup(List<String> args);

    Response xreadgroupAndAwait(List<String> args);

    Uni<Response> xrevrange(List<String> args);

    Response xrevrangeAndAwait(List<String> args);

    Uni<Response> xsetid(String arg0, String arg1);

    Response xsetidAndAwait(String arg0, String arg1);

    Uni<Response> xtrim(List<String> args);

    Response xtrimAndAwait(List<String> args);

    Uni<Response> zadd(List<String> args);

    Response zaddAndAwait(List<String> args);

    Uni<Response> zcard(String arg0);

    Response zcardAndAwait(String arg0);

    Uni<Response> zcount(String arg0, String arg1, String arg2);

    Response zcountAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zincrby(String arg0, String arg1, String arg2);

    Response zincrbyAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zinterstore(List<String> args);

    Response zinterstoreAndAwait(List<String> args);

    Uni<Response> zlexcount(String arg0, String arg1, String arg2);

    Response zlexcountAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zpopmax(List<String> args);

    Response zpopmaxAndAwait(List<String> args);

    Uni<Response> zpopmin(List<String> args);

    Response zpopminAndAwait(List<String> args);

    Uni<Response> zrange(List<String> args);

    Response zrangeAndAwait(List<String> args);

    Uni<Response> zrangebylex(List<String> args);

    Response zrangebylexAndAwait(List<String> args);

    Uni<Response> zrangebyscore(List<String> args);

    Response zrangebyscoreAndAwait(List<String> args);

    Uni<Response> zrank(String arg0, String arg1);

    Response zrankAndAwait(String arg0, String arg1);

    Uni<Response> zrem(List<String> args);

    Response zremAndAwait(List<String> args);

    Uni<Response> zremrangebylex(String arg0, String arg1, String arg2);

    Response zremrangebylexAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zremrangebyrank(String arg0, String arg1, String arg2);

    Response zremrangebyrankAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zremrangebyscore(String arg0, String arg1, String arg2);

    Response zremrangebyscoreAndAwait(String arg0, String arg1, String arg2);

    Uni<Response> zrevrange(List<String> args);

    Response zrevrangeAndAwait(List<String> args);

    Uni<Response> zrevrangebylex(List<String> args);

    Response zrevrangebylexAndAwait(List<String> args);

    Uni<Response> zrevrangebyscore(List<String> args);

    Response zrevrangebyscoreAndAwait(List<String> args);

    Uni<Response> zrevrank(String arg0, String arg1);

    Response zrevrankAndAwait(String arg0, String arg1);

    Uni<Response> zscan(List<String> args);

    Response zscanAndAwait(List<String> args);

    Uni<Response> zscore(String arg0, String arg1);

    Response zscoreAndAwait(String arg0, String arg1);

    Uni<Response> zunionstore(List<String> args);

    Response zunionstoreAndAwait(List<String> args);
}
