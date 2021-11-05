package io.quarkus.redis.client.runtime;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.client.RedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

class RedisClientImpl implements RedisClient {
    private final RedisAPI redisAPI;
    private final Duration timeout;

    public RedisClientImpl(RedisAPI redisAPI, Duration timeout) {
        this.redisAPI = redisAPI;
        this.timeout = timeout;
    }

    @Override
    public void close() {
        redisAPI.close();
    }

    @Override
    public Response append(String arg0, String arg1) {
        return await(redisAPI.append(arg0, arg1));
    }

    @Override
    public Response asking() {
        return await(redisAPI.asking());
    }

    @Override
    public Response auth(List<String> args) {
        return await(redisAPI.auth(args));
    }

    @Override
    public Response bgrewriteaof() {
        return await(redisAPI.bgrewriteaof());
    }

    @Override
    public Response bgsave(List<String> args) {
        return await(redisAPI.bgsave(args));
    }

    @Override
    public Response bitcount(List<String> args) {
        return await(redisAPI.bitcount(args));
    }

    @Override
    public Response bitfield(List<String> args) {
        return await(redisAPI.bitfield(args));
    }

    @Override
    public Response bitop(List<String> args) {
        return await(redisAPI.bitop(args));
    }

    @Override
    public Response bitpos(List<String> args) {
        return await(redisAPI.bitpos(args));
    }

    @Override
    public Response blpop(List<String> args) {
        return await(redisAPI.blpop(args));
    }

    @Override
    public Response brpop(List<String> args) {
        return await(redisAPI.brpop(args));
    }

    @Override
    public Response brpoplpush(String arg0, String arg1, String arg2) {
        return await(redisAPI.brpoplpush(arg0, arg1, arg2));
    }

    @Override
    public Response bzpopmax(List<String> args) {
        return await(redisAPI.bzpopmax(args));
    }

    @Override
    public Response bzpopmin(List<String> args) {
        return await(redisAPI.bzpopmin(args));
    }

    @Override
    public Response client(List<String> args) {
        return await(redisAPI.client(args));
    }

    @Override
    public Response cluster(List<String> args) {
        return await(redisAPI.cluster(args));
    }

    @Override
    public Response command(List<String> args) {
        return await(redisAPI.command(args));
    }

    @Override
    public Response config(List<String> args) {
        return await(redisAPI.config(args));
    }

    @Override
    public Response dbsize() {
        return await(redisAPI.dbsize());
    }

    @Override
    public Response debug(List<String> args) {
        return await(redisAPI.debug(args));
    }

    @Override
    public Response decr(String arg0) {
        return await(redisAPI.decr(arg0));
    }

    @Override
    public Response decrby(String arg0, String arg1) {
        return await(redisAPI.decrby(arg0, arg1));
    }

    @Override
    public Response del(List<String> args) {
        return await(redisAPI.del(args));
    }

    @Override
    public Response discard() {
        return await(redisAPI.discard());
    }

    @Override
    public Response dump(String arg0) {
        return await(redisAPI.dump(arg0));
    }

    @Override
    public Response echo(String arg0) {
        return await(redisAPI.echo(arg0));
    }

    @Override
    public Response eval(List<String> args) {
        return await(redisAPI.eval(args));
    }

    @Override
    public Response evalsha(List<String> args) {
        return await(redisAPI.evalsha(args));
    }

    @Override
    public Response exec() {
        return await(redisAPI.exec());
    }

    @Override
    public Response exists(List<String> args) {
        return await(redisAPI.exists(args));
    }

    @Override
    public Response expire(String arg0, String arg1) {
        return await(redisAPI.expire(arg0, arg1));
    }

    @Override
    public Response expireat(String arg0, String arg1) {
        return await(redisAPI.expireat(arg0, arg1));
    }

    @Override
    public Response flushall(List<String> args) {
        return await(redisAPI.flushall(args));
    }

    @Override
    public Response flushdb(List<String> args) {
        return await(redisAPI.flushdb(args));
    }

    @Override
    public Response geoadd(List<String> args) {
        return await(redisAPI.geoadd(args));
    }

    @Override
    public Response geodist(List<String> args) {
        return await(redisAPI.geodist(args));
    }

    @Override
    public Response geohash(List<String> args) {
        return await(redisAPI.geohash(args));
    }

    @Override
    public Response geopos(List<String> args) {
        return await(redisAPI.geopos(args));
    }

    @Override
    public Response georadius(List<String> args) {
        return await(redisAPI.georadius(args));
    }

    @Override
    public Response georadiusRo(List<String> args) {
        return await(redisAPI.georadiusRo(args));
    }

    @Override
    public Response georadiusbymember(List<String> args) {
        return await(redisAPI.georadiusbymember(args));
    }

    @Override
    public Response georadiusbymemberRo(List<String> args) {
        return await(redisAPI.georadiusbymemberRo(args));
    }

    @Override
    public Response get(String arg0) {
        return await(redisAPI.get(arg0));
    }

    @Override
    public Response getbit(String arg0, String arg1) {
        return await(redisAPI.getbit(arg0, arg1));
    }

    @Override
    public Response getrange(String arg0, String arg1, String arg2) {
        return await(redisAPI.getrange(arg0, arg1, arg2));
    }

    @Override
    public Response getset(String arg0, String arg1) {
        return await(redisAPI.getset(arg0, arg1));
    }

    @Override
    public Response hdel(List<String> args) {
        return await(redisAPI.hdel(args));
    }

    @Override
    public Response hexists(String arg0, String arg1) {
        return await(redisAPI.hexists(arg0, arg1));
    }

    @Override
    public Response hget(String arg0, String arg1) {
        return await(redisAPI.hget(arg0, arg1));
    }

    @Override
    public Response hgetall(String arg0) {
        return await(redisAPI.hgetall(arg0));
    }

    @Override
    public Response hincrby(String arg0, String arg1, String arg2) {
        return await(redisAPI.hincrby(arg0, arg1, arg2));
    }

    @Override
    public Response hincrbyfloat(String arg0, String arg1, String arg2) {
        return await(redisAPI.hincrbyfloat(arg0, arg1, arg2));
    }

    @Override
    public Response hkeys(String arg0) {
        return await(redisAPI.hkeys(arg0));
    }

    @Override
    public Response hlen(String arg0) {
        return await(redisAPI.hlen(arg0));
    }

    @Override
    public Response hmget(List<String> args) {
        return await(redisAPI.hmget(args));
    }

    @Override
    public Response hmset(List<String> args) {
        return await(redisAPI.hmset(args));
    }

    @Override
    public Response host(List<String> args) {
        return await(redisAPI.host(args));
    }

    @Override
    public Response hscan(List<String> args) {
        return await(redisAPI.hscan(args));
    }

    @Override
    public Response hset(List<String> args) {
        return await(redisAPI.hset(args));
    }

    @Override
    public Response hsetnx(String arg0, String arg1, String arg2) {
        return await(redisAPI.hsetnx(arg0, arg1, arg2));
    }

    @Override
    public Response hstrlen(String arg0, String arg1) {
        return await(redisAPI.hstrlen(arg0, arg1));
    }

    @Override
    public Response hvals(String arg0) {
        return await(redisAPI.hvals(arg0));
    }

    @Override
    public Response incr(String arg0) {
        return await(redisAPI.incr(arg0));
    }

    @Override
    public Response incrby(String arg0, String arg1) {
        return await(redisAPI.incrby(arg0, arg1));
    }

    @Override
    public Response incrbyfloat(String arg0, String arg1) {
        return await(redisAPI.incrbyfloat(arg0, arg1));
    }

    @Override
    public Response info(List<String> args) {
        return await(redisAPI.info(args));
    }

    @Override
    public Response keys(String arg0) {
        return await(redisAPI.keys(arg0));
    }

    @Override
    public Response lastsave() {
        return await(redisAPI.lastsave());
    }

    @Override
    public Response latency(List<String> args) {
        return await(redisAPI.latency(args));
    }

    @Override
    public Response lindex(String arg0, String arg1) {
        return await(redisAPI.lindex(arg0, arg1));
    }

    @Override
    public Response linsert(String arg0, String arg1, String arg2, String arg3) {
        return await(redisAPI.linsert(arg0, arg1, arg2, arg3));
    }

    @Override
    public Response llen(String arg0) {
        return await(redisAPI.llen(arg0));
    }

    @Override
    public Response lolwut(List<String> args) {
        return await(redisAPI.lolwut(args));
    }

    @Override
    public Response lpop(String arg0) {
        return await(redisAPI.lpop(List.of(arg0)));
    }

    @Override
    public Response lpop(List<String> args) {
        return await(redisAPI.lpop(args));
    }

    @Override
    public Response lpush(List<String> args) {
        return await(redisAPI.lpush(args));
    }

    @Override
    public Response lpushx(List<String> args) {
        return await(redisAPI.lpushx(args));
    }

    @Override
    public Response lrange(String arg0, String arg1, String arg2) {
        return await(redisAPI.lrange(arg0, arg1, arg2));
    }

    @Override
    public Response lrem(String arg0, String arg1, String arg2) {
        return await(redisAPI.lrem(arg0, arg1, arg2));
    }

    @Override
    public Response lset(String arg0, String arg1, String arg2) {
        return await(redisAPI.lset(arg0, arg1, arg2));
    }

    @Override
    public Response ltrim(String arg0, String arg1, String arg2) {
        return await(redisAPI.ltrim(arg0, arg1, arg2));
    }

    @Override
    public Response memory(List<String> args) {
        return await(redisAPI.memory(args));
    }

    @Override
    public Response mget(List<String> args) {
        return await(redisAPI.mget(args));
    }

    @Override
    public Response migrate(List<String> args) {
        return await(redisAPI.migrate(args));
    }

    @Override
    public Response module(List<String> args) {
        return await(redisAPI.module(args));
    }

    @Override
    public Response monitor() {
        return await(redisAPI.monitor());
    }

    @Override
    public Response move(String arg0, String arg1) {
        return await(redisAPI.move(arg0, arg1));
    }

    @Override
    public Response mset(List<String> args) {
        return await(redisAPI.mset(args));
    }

    @Override
    public Response msetnx(List<String> args) {
        return await(redisAPI.msetnx(args));
    }

    @Override
    public Response multi() {
        return await(redisAPI.multi());
    }

    @Override
    public Response object(List<String> args) {
        return await(redisAPI.object(args));
    }

    @Override
    public Response persist(String arg0) {
        return await(redisAPI.persist(arg0));
    }

    @Override
    public Response pexpire(String arg0, String arg1) {
        return await(redisAPI.pexpire(arg0, arg1));
    }

    @Override
    public Response pexpireat(String arg0, String arg1) {
        return await(redisAPI.pexpireat(arg0, arg1));
    }

    @Override
    public Response pfadd(List<String> args) {
        return await(redisAPI.pfadd(args));
    }

    @Override
    public Response pfcount(List<String> args) {
        return await(redisAPI.pfcount(args));
    }

    @Override
    public Response pfdebug(List<String> args) {
        return await(redisAPI.pfdebug(args));
    }

    @Override
    public Response pfmerge(List<String> args) {
        return await(redisAPI.pfmerge(args));
    }

    @Override
    public Response pfselftest() {
        return await(redisAPI.pfselftest());
    }

    @Override
    public Response ping(List<String> args) {
        return await(redisAPI.ping(args));
    }

    @Override
    public Response post(List<String> args) {
        return await(redisAPI.post(args));
    }

    @Override
    public Response psetex(String arg0, String arg1, String arg2) {
        return await(redisAPI.psetex(arg0, arg1, arg2));
    }

    @Override
    public Response psubscribe(List<String> args) {
        return await(redisAPI.psubscribe(args));
    }

    @Override
    public Response psync(String arg0, String arg1) {
        return await(redisAPI.psync(List.of(arg0, arg1)));
    }

    @Override
    public Response psync(List<String> args) {
        return await(redisAPI.psync(args));
    }

    @Override
    public Response pttl(String arg0) {
        return await(redisAPI.pttl(arg0));
    }

    @Override
    public Response publish(String arg0, String arg1) {
        return await(redisAPI.publish(arg0, arg1));
    }

    @Override
    public Response pubsub(List<String> args) {
        return await(redisAPI.pubsub(args));
    }

    @Override
    public Response punsubscribe(List<String> args) {
        return await(redisAPI.punsubscribe(args));
    }

    @Override
    public Response randomkey() {
        return await(redisAPI.randomkey());
    }

    @Override
    public Response readonly() {
        return await(redisAPI.readonly());
    }

    @Override
    public Response readwrite() {
        return await(redisAPI.readwrite());
    }

    @Override
    public Response rename(String arg0, String arg1) {
        return await(redisAPI.rename(arg0, arg1));
    }

    @Override
    public Response renamenx(String arg0, String arg1) {
        return await(redisAPI.renamenx(arg0, arg1));
    }

    @Override
    public Response replconf(List<String> args) {
        return await(redisAPI.replconf(args));
    }

    @Override
    public Response replicaof(String arg0, String arg1) {
        return await(redisAPI.replicaof(arg0, arg1));
    }

    @Override
    public Response restore(List<String> args) {
        return await(redisAPI.restore(args));
    }

    @Override
    public Response restoreAsking(List<String> args) {
        return await(redisAPI.restoreAsking(args));
    }

    @Override
    public Response role() {
        return await(redisAPI.role());
    }

    @Override
    public Response rpop(String arg0) {
        return await(redisAPI.rpop(List.of(arg0)));
    }

    @Override
    public Response rpop(List<String> args) {
        return await(redisAPI.rpop(args));
    }

    @Override
    public Response rpoplpush(String arg0, String arg1) {
        return await(redisAPI.rpoplpush(arg0, arg1));
    }

    @Override
    public Response rpush(List<String> args) {
        return await(redisAPI.rpush(args));
    }

    @Override
    public Response rpushx(List<String> args) {
        return await(redisAPI.rpushx(args));
    }

    @Override
    public Response sadd(List<String> args) {
        return await(redisAPI.sadd(args));
    }

    @Override
    public Response save() {
        return await(redisAPI.save());
    }

    @Override
    public Response scan(List<String> args) {
        return await(redisAPI.scan(args));
    }

    @Override
    public Response scard(String arg0) {
        return await(redisAPI.scard(arg0));
    }

    @Override
    public Response script(List<String> args) {
        return await(redisAPI.script(args));
    }

    @Override
    public Response sdiff(List<String> args) {
        return await(redisAPI.sdiff(args));
    }

    @Override
    public Response sdiffstore(List<String> args) {
        return await(redisAPI.sdiffstore(args));
    }

    @Override
    public Response select(String arg0) {
        return await(redisAPI.select(arg0));
    }

    @Override
    public Response set(List<String> args) {
        return await(redisAPI.set(args));
    }

    @Override
    public Response setbit(String arg0, String arg1, String arg2) {
        return await(redisAPI.setbit(arg0, arg1, arg2));
    }

    @Override
    public Response setex(String arg0, String arg1, String arg2) {
        return await(redisAPI.setex(arg0, arg1, arg2));
    }

    @Override
    public Response setnx(String arg0, String arg1) {
        return await(redisAPI.setnx(arg0, arg1));
    }

    @Override
    public Response setrange(String arg0, String arg1, String arg2) {
        return await(redisAPI.setrange(arg0, arg1, arg2));
    }

    @Override
    public Response shutdown(List<String> args) {
        return await(redisAPI.shutdown(args));
    }

    @Override
    public Response sinter(List<String> args) {
        return await(redisAPI.sinter(args));
    }

    @Override
    public Response sinterstore(List<String> args) {
        return await(redisAPI.sinterstore(args));
    }

    @Override
    public Response sismember(String arg0, String arg1) {
        return await(redisAPI.sismember(arg0, arg1));
    }

    @Override
    public Response slaveof(String arg0, String arg1) {
        return await(redisAPI.slaveof(arg0, arg1));
    }

    @Override
    public Response slowlog(List<String> args) {
        return await(redisAPI.slowlog(args));
    }

    @Override
    public Response smembers(String arg0) {
        return await(redisAPI.smembers(arg0));
    }

    @Override
    public Response smove(String arg0, String arg1, String arg2) {
        return await(redisAPI.smove(arg0, arg1, arg2));
    }

    @Override
    public Response sort(List<String> args) {
        return await(redisAPI.sort(args));
    }

    @Override
    public Response spop(List<String> args) {
        return await(redisAPI.spop(args));
    }

    @Override
    public Response srandmember(List<String> args) {
        return await(redisAPI.srandmember(args));
    }

    @Override
    public Response srem(List<String> args) {
        return await(redisAPI.srem(args));
    }

    @Override
    public Response sscan(List<String> args) {
        return await(redisAPI.sscan(args));
    }

    @Override
    public Response strlen(String arg0) {
        return await(redisAPI.strlen(arg0));
    }

    @Override
    public Response subscribe(List<String> args) {
        return await(redisAPI.subscribe(args));
    }

    @Override
    public Response substr(String arg0, String arg1, String arg2) {
        return await(redisAPI.substr(arg0, arg1, arg2));
    }

    @Override
    public Response sunion(List<String> args) {
        return await(redisAPI.sunion(args));
    }

    @Override
    public Response sunionstore(List<String> args) {
        return await(redisAPI.sunionstore(args));
    }

    @Override
    public Response swapdb(String arg0, String arg1) {
        return await(redisAPI.swapdb(arg0, arg1));
    }

    @Override
    public Response sync() {
        return await(redisAPI.sync());
    }

    @Override
    public Response time() {
        return await(redisAPI.time());
    }

    @Override
    public Response touch(List<String> args) {
        return await(redisAPI.touch(args));
    }

    @Override
    public Response ttl(String arg0) {
        return await(redisAPI.ttl(arg0));
    }

    @Override
    public Response type(String arg0) {
        return await(redisAPI.type(arg0));
    }

    @Override
    public Response unlink(List<String> args) {
        return await(redisAPI.unlink(args));
    }

    @Override
    public Response unsubscribe(List<String> args) {
        return await(redisAPI.unsubscribe(args));
    }

    @Override
    public Response unwatch() {
        return await(redisAPI.unwatch());
    }

    @Override
    public Response wait(String arg0, String arg1) {
        return await(redisAPI.wait(arg0, arg1));
    }

    @Override
    public Response watch(List<String> args) {
        return await(redisAPI.watch(args));
    }

    @Override
    public Response xack(List<String> args) {
        return await(redisAPI.xack(args));
    }

    @Override
    public Response xadd(List<String> args) {
        return await(redisAPI.xadd(args));
    }

    @Override
    public Response xclaim(List<String> args) {
        return await(redisAPI.xclaim(args));
    }

    @Override
    public Response xdel(List<String> args) {
        return await(redisAPI.xdel(args));
    }

    @Override
    public Response xgroup(List<String> args) {
        return await(redisAPI.xgroup(args));
    }

    @Override
    public Response xinfo(List<String> args) {
        return await(redisAPI.xinfo(args));
    }

    @Override
    public Response xlen(String arg0) {
        return await(redisAPI.xlen(arg0));
    }

    @Override
    public Response xpending(List<String> args) {
        return await(redisAPI.xpending(args));
    }

    @Override
    public Response xrange(List<String> args) {
        return await(redisAPI.xrange(args));
    }

    @Override
    public Response xread(List<String> args) {
        return await(redisAPI.xread(args));
    }

    @Override
    public Response xreadgroup(List<String> args) {
        return await(redisAPI.xreadgroup(args));
    }

    @Override
    public Response xrevrange(List<String> args) {
        return await(redisAPI.xrevrange(args));
    }

    @Override
    public Response xsetid(String arg0, String arg1) {
        return await(redisAPI.xsetid(arg0, arg1));
    }

    @Override
    public Response xtrim(List<String> args) {
        return await(redisAPI.xtrim(args));
    }

    @Override
    public Response zadd(List<String> args) {
        return await(redisAPI.zadd(args));
    }

    @Override
    public Response zcard(String arg0) {
        return await(redisAPI.zcard(arg0));
    }

    @Override
    public Response zcount(String arg0, String arg1, String arg2) {
        return await(redisAPI.zcount(arg0, arg1, arg2));
    }

    @Override
    public Response zincrby(String arg0, String arg1, String arg2) {
        return await(redisAPI.zincrby(arg0, arg1, arg2));
    }

    @Override
    public Response zinterstore(List<String> args) {
        return await(redisAPI.zinterstore(args));
    }

    @Override
    public Response zlexcount(String arg0, String arg1, String arg2) {
        return await(redisAPI.zlexcount(arg0, arg1, arg2));
    }

    @Override
    public Response zpopmax(List<String> args) {
        return await(redisAPI.zpopmax(args));
    }

    @Override
    public Response zpopmin(List<String> args) {
        return await(redisAPI.zpopmin(args));
    }

    @Override
    public Response zrange(List<String> args) {
        return await(redisAPI.zrange(args));
    }

    @Override
    public Response zrangebylex(List<String> args) {
        return await(redisAPI.zrangebylex(args));
    }

    @Override
    public Response zrangebyscore(List<String> args) {
        return await(redisAPI.zrangebyscore(args));
    }

    @Override
    public Response zrank(String arg0, String arg1) {
        return await(redisAPI.zrank(arg0, arg1));
    }

    @Override
    public Response zrem(List<String> args) {
        return await(redisAPI.zrem(args));
    }

    @Override
    public Response zremrangebylex(String arg0, String arg1, String arg2) {
        return await(redisAPI.zremrangebylex(arg0, arg1, arg2));
    }

    @Override
    public Response zremrangebyrank(String arg0, String arg1, String arg2) {
        return await(redisAPI.zremrangebyrank(arg0, arg1, arg2));
    }

    @Override
    public Response zremrangebyscore(String arg0, String arg1, String arg2) {
        return await(redisAPI.zremrangebyscore(arg0, arg1, arg2));
    }

    @Override
    public Response zrevrange(List<String> args) {
        return await(redisAPI.zrevrange(args));
    }

    @Override
    public Response zrevrangebylex(List<String> args) {
        return await(redisAPI.zrevrangebylex(args));
    }

    @Override
    public Response zrevrangebyscore(List<String> args) {
        return await(redisAPI.zrevrangebyscore(args));
    }

    @Override
    public Response zrevrank(String arg0, String arg1) {
        return await(redisAPI.zrevrank(arg0, arg1));
    }

    @Override
    public Response zscan(List<String> args) {
        return await(redisAPI.zscan(args));
    }

    @Override
    public Response zscore(String arg0, String arg1) {
        return await(redisAPI.zscore(arg0, arg1));
    }

    @Override
    public Response zunionstore(List<String> args) {
        return await(redisAPI.zunionstore(args));
    }

    private Response await(Uni<io.vertx.mutiny.redis.client.Response> mutinyResponse) {
        io.vertx.mutiny.redis.client.Response response = mutinyResponse.await().atMost(timeout);
        if (response == null) {
            return null;
        }
        return response.getDelegate();
    }
}
