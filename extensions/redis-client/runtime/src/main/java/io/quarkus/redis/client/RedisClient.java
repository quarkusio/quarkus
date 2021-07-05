package io.quarkus.redis.client;

import static io.quarkus.redis.client.runtime.RedisClientUtil.DEFAULT_CLIENT;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.runtime.RedisClientsProducer;
import io.vertx.redis.client.Response;

/**
 * A synchronous Redis client offering blocking Redis commands.
 * The commands have a default timeout of 10 seconds which can be configured
 * via {@code quarkus.redis.timeout} configuration knob.
 *
 * For more information about how each individual command visit
 * the <a href="https://redis.io/commands">Redis Commands Page</a>
 */
public interface RedisClient {
    /**
     * Creates the {@link RedisClient} using the default redis client configuration
     * 
     * @return {@link RedisClient} - the default redis client
     */
    static RedisClient createClient() {
        return createClient(DEFAULT_CLIENT);
    }

    /**
     * Creates the {@link RedisClient} using the named redis client configuration
     * 
     * @return {@link RedisClient} - the named client
     */
    static RedisClient createClient(String name) {
        RedisClientsProducer redisClientsProducer = Arc.container().instance(RedisClientsProducer.class).get();
        return redisClientsProducer.getRedisClient(name);
    }

    void close();

    Response append(String arg0, String arg1);

    Response asking();

    Response auth(List<String> args);

    Response bgrewriteaof();

    Response bgsave(List<String> args);

    Response bitcount(List<String> args);

    Response bitfield(List<String> args);

    Response bitop(List<String> args);

    Response bitpos(List<String> args);

    Response blpop(List<String> args);

    Response brpop(List<String> args);

    Response brpoplpush(String arg0, String arg1, String arg2);

    Response bzpopmax(List<String> args);

    Response bzpopmin(List<String> args);

    Response client(List<String> args);

    Response cluster(List<String> args);

    Response command(List<String> args);

    Response config(List<String> args);

    Response dbsize();

    Response debug(List<String> args);

    Response decr(String arg0);

    Response decrby(String arg0, String arg1);

    Response del(List<String> args);

    Response discard();

    Response dump(String arg0);

    Response echo(String arg0);

    Response eval(List<String> args);

    Response evalsha(List<String> args);

    Response exec();

    Response exists(List<String> args);

    Response expire(String arg0, String arg1);

    Response expireat(String arg0, String arg1);

    Response flushall(List<String> args);

    Response flushdb(List<String> args);

    Response geoadd(List<String> args);

    Response geodist(List<String> args);

    Response geohash(List<String> args);

    Response geopos(List<String> args);

    Response georadius(List<String> args);

    Response georadiusRo(List<String> args);

    Response georadiusbymember(List<String> args);

    Response georadiusbymemberRo(List<String> args);

    Response get(String arg0);

    Response getbit(String arg0, String arg1);

    Response getrange(String arg0, String arg1, String arg2);

    Response getset(String arg0, String arg1);

    Response hdel(List<String> args);

    Response hexists(String arg0, String arg1);

    Response hget(String arg0, String arg1);

    Response hgetall(String arg0);

    Response hincrby(String arg0, String arg1, String arg2);

    Response hincrbyfloat(String arg0, String arg1, String arg2);

    Response hkeys(String arg0);

    Response hlen(String arg0);

    Response hmget(List<String> args);

    Response hmset(List<String> args);

    Response host(List<String> args);

    Response hscan(List<String> args);

    Response hset(List<String> args);

    Response hsetnx(String arg0, String arg1, String arg2);

    Response hstrlen(String arg0, String arg1);

    Response hvals(String arg0);

    Response incr(String arg0);

    Response incrby(String arg0, String arg1);

    Response incrbyfloat(String arg0, String arg1);

    Response info(List<String> args);

    Response keys(String arg0);

    Response lastsave();

    Response latency(List<String> args);

    Response lindex(String arg0, String arg1);

    Response linsert(String arg0, String arg1, String arg2, String arg3);

    Response llen(String arg0);

    Response lolwut(List<String> args);

    Response lpop(String arg0);

    Response lpush(List<String> args);

    Response lpushx(List<String> args);

    Response lrange(String arg0, String arg1, String arg2);

    Response lrem(String arg0, String arg1, String arg2);

    Response lset(String arg0, String arg1, String arg2);

    Response ltrim(String arg0, String arg1, String arg2);

    Response memory(List<String> args);

    Response mget(List<String> args);

    Response migrate(List<String> args);

    Response module(List<String> args);

    Response monitor();

    Response move(String arg0, String arg1);

    Response mset(List<String> args);

    Response msetnx(List<String> args);

    Response multi();

    Response object(List<String> args);

    Response persist(String arg0);

    Response pexpire(String arg0, String arg1);

    Response pexpireat(String arg0, String arg1);

    Response pfadd(List<String> args);

    Response pfcount(List<String> args);

    Response pfdebug(List<String> args);

    Response pfmerge(List<String> args);

    Response pfselftest();

    Response ping(List<String> args);

    Response post(List<String> args);

    Response psetex(String arg0, String arg1, String arg2);

    Response psubscribe(List<String> args);

    Response psync(String arg0, String arg1);

    Response pttl(String arg0);

    Response publish(String arg0, String arg1);

    Response pubsub(List<String> args);

    Response punsubscribe(List<String> args);

    Response randomkey();

    Response readonly();

    Response readwrite();

    Response rename(String arg0, String arg1);

    Response renamenx(String arg0, String arg1);

    Response replconf(List<String> args);

    Response replicaof(String arg0, String arg1);

    Response restore(List<String> args);

    Response restoreAsking(List<String> args);

    Response role();

    Response rpop(String arg0);

    Response rpoplpush(String arg0, String arg1);

    Response rpush(List<String> args);

    Response rpushx(List<String> args);

    Response sadd(List<String> args);

    Response save();

    Response scan(List<String> args);

    Response scard(String arg0);

    Response script(List<String> args);

    Response sdiff(List<String> args);

    Response sdiffstore(List<String> args);

    Response select(String arg0);

    Response set(List<String> args);

    Response setbit(String arg0, String arg1, String arg2);

    Response setex(String arg0, String arg1, String arg2);

    Response setnx(String arg0, String arg1);

    Response setrange(String arg0, String arg1, String arg2);

    Response shutdown(List<String> args);

    Response sinter(List<String> args);

    Response sinterstore(List<String> args);

    Response sismember(String arg0, String arg1);

    Response slaveof(String arg0, String arg1);

    Response slowlog(List<String> args);

    Response smembers(String arg0);

    Response smove(String arg0, String arg1, String arg2);

    Response sort(List<String> args);

    Response spop(List<String> args);

    Response srandmember(List<String> args);

    Response srem(List<String> args);

    Response sscan(List<String> args);

    Response strlen(String arg0);

    Response subscribe(List<String> args);

    Response substr(String arg0, String arg1, String arg2);

    Response sunion(List<String> args);

    Response sunionstore(List<String> args);

    Response swapdb(String arg0, String arg1);

    Response sync();

    Response time();

    Response touch(List<String> args);

    Response ttl(String arg0);

    Response type(String arg0);

    Response unlink(List<String> args);

    Response unsubscribe(List<String> args);

    Response unwatch();

    Response wait(String arg0, String arg1);

    Response watch(List<String> args);

    Response xack(List<String> args);

    Response xadd(List<String> args);

    Response xclaim(List<String> args);

    Response xdel(List<String> args);

    Response xgroup(List<String> args);

    Response xinfo(List<String> args);

    Response xlen(String arg0);

    Response xpending(List<String> args);

    Response xrange(List<String> args);

    Response xread(List<String> args);

    Response xreadgroup(List<String> args);

    Response xrevrange(List<String> args);

    Response xsetid(String arg0, String arg1);

    Response xtrim(List<String> args);

    Response zadd(List<String> args);

    Response zcard(String arg0);

    Response zcount(String arg0, String arg1, String arg2);

    Response zincrby(String arg0, String arg1, String arg2);

    Response zinterstore(List<String> args);

    Response zlexcount(String arg0, String arg1, String arg2);

    Response zpopmax(List<String> args);

    Response zpopmin(List<String> args);

    Response zrange(List<String> args);

    Response zrangebylex(List<String> args);

    Response zrangebyscore(List<String> args);

    Response zrank(String arg0, String arg1);

    Response zrem(List<String> args);

    Response zremrangebylex(String arg0, String arg1, String arg2);

    Response zremrangebyrank(String arg0, String arg1, String arg2);

    Response zremrangebyscore(String arg0, String arg1, String arg2);

    Response zrevrange(List<String> args);

    Response zrevrangebylex(List<String> args);

    Response zrevrangebyscore(List<String> args);

    Response zrevrank(String arg0, String arg1);

    Response zscan(List<String> args);

    Response zscore(String arg0, String arg1);

    Response zunion(List<String> args);

    Response zunionstore(List<String> args);
}
