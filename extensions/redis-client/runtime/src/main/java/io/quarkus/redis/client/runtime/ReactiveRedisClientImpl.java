package io.quarkus.redis.client.runtime;

import java.util.List;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Response;

class ReactiveRedisClientImpl implements ReactiveRedisClient {
    private final RedisAPI redisAPI;

    public ReactiveRedisClientImpl(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Override
    public void close() {
        redisAPI.close();
    }

    @Override
    public Uni<Response> append(String arg0, String arg1) {
        return redisAPI.append(arg0, arg1);
    }

    @Override
    public Response appendAndAwait(String arg0, String arg1) {
        return redisAPI.appendAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> asking() {
        return redisAPI.asking();
    }

    @Override
    public Response askingAndAwait() {
        return redisAPI.askingAndAwait();
    }

    @Override
    public Uni<Response> auth(List<String> args) {
        return redisAPI.auth(args);
    }

    @Override
    public Response authAndAwait(List<String> args) {
        return redisAPI.authAndAwait(args);
    }

    @Override
    public Uni<Response> bgrewriteaof() {
        return redisAPI.bgrewriteaof();
    }

    @Override
    public Response bgrewriteaofAndAwait() {
        return redisAPI.bgrewriteaofAndAwait();
    }

    @Override
    public Uni<Response> bgsave(List<String> args) {
        return redisAPI.bgsave(args);
    }

    @Override
    public Response bgsaveAndAwait(List<String> args) {
        return redisAPI.bgsaveAndAwait(args);
    }

    @Override
    public Uni<Response> bitcount(List<String> args) {
        return redisAPI.bitcount(args);
    }

    @Override
    public Response bitcountAndAwait(List<String> args) {
        return redisAPI.bitcountAndAwait(args);
    }

    @Override
    public Uni<Response> bitfield(List<String> args) {
        return redisAPI.bitfield(args);
    }

    @Override
    public Response bitfieldAndAwait(List<String> args) {
        return redisAPI.bitfieldAndAwait(args);
    }

    @Override
    public Uni<Response> bitop(List<String> args) {
        return redisAPI.bitop(args);
    }

    @Override
    public Response bitopAndAwait(List<String> args) {
        return redisAPI.bitopAndAwait(args);
    }

    @Override
    public Uni<Response> bitpos(List<String> args) {
        return redisAPI.bitpos(args);
    }

    @Override
    public Response bitposAndAwait(List<String> args) {
        return redisAPI.bitposAndAwait(args);
    }

    @Override
    public Uni<Response> blpop(List<String> args) {
        return redisAPI.blpop(args);
    }

    @Override
    public Response blpopAndAwait(List<String> args) {
        return redisAPI.blpopAndAwait(args);
    }

    @Override
    public Uni<Response> brpop(List<String> args) {
        return redisAPI.brpop(args);
    }

    @Override
    public Response brpopAndAwait(List<String> args) {
        return redisAPI.brpopAndAwait(args);
    }

    @Override
    public Uni<Response> brpoplpush(String arg0, String arg1, String arg2) {
        return redisAPI.brpoplpush(arg0, arg1, arg2);
    }

    @Override
    public Response brpoplpushAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.brpoplpushAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> bzpopmax(List<String> args) {
        return redisAPI.bzpopmax(args);
    }

    @Override
    public Response bzpopmaxAndAwait(List<String> args) {
        return redisAPI.bzpopmaxAndAwait(args);
    }

    @Override
    public Uni<Response> bzpopmin(List<String> args) {
        return redisAPI.bzpopmin(args);
    }

    @Override
    public Response bzpopminAndAwait(List<String> args) {
        return redisAPI.bzpopminAndAwait(args);
    }

    @Override
    public Uni<Response> client(List<String> args) {
        return redisAPI.client(args);
    }

    @Override
    public Response clientAndAwait(List<String> args) {
        return redisAPI.clientAndAwait(args);
    }

    @Override
    public Uni<Response> cluster(List<String> args) {
        return redisAPI.cluster(args);
    }

    @Override
    public Response clusterAndAwait(List<String> args) {
        return redisAPI.clusterAndAwait(args);
    }

    @Override
    public Uni<Response> command(List<String> args) {
        return redisAPI.command(args);
    }

    @Override
    public Response commandAndAwait(List<String> args) {
        return redisAPI.commandAndAwait(args);
    }

    @Override
    public Uni<Response> config(List<String> args) {
        return redisAPI.config(args);
    }

    @Override
    public Response configAndAwait(List<String> args) {
        return redisAPI.configAndAwait(args);
    }

    @Override
    public Uni<Response> dbsize() {
        return redisAPI.dbsize();
    }

    @Override
    public Response dbsizeAndAwait() {
        return redisAPI.dbsizeAndAwait();
    }

    @Override
    public Uni<Response> debug(List<String> args) {
        return redisAPI.debug(args);
    }

    @Override
    public Response debugAndAwait(List<String> args) {
        return redisAPI.debugAndAwait(args);
    }

    @Override
    public Uni<Response> decr(String arg0) {
        return redisAPI.decr(arg0);
    }

    @Override
    public Response decrAndAwait(String arg0) {
        return redisAPI.decrAndAwait(arg0);
    }

    @Override
    public Uni<Response> decrby(String arg0, String arg1) {
        return redisAPI.decrby(arg0, arg1);
    }

    @Override
    public Response decrbyAndAwait(String arg0, String arg1) {
        return redisAPI.decrbyAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> del(List<String> args) {
        return redisAPI.del(args);
    }

    @Override
    public Response delAndAwait(List<String> args) {
        return redisAPI.delAndAwait(args);
    }

    @Override
    public Uni<Response> discard() {
        return redisAPI.discard();
    }

    @Override
    public Response discardAndAwait() {
        return redisAPI.discardAndAwait();
    }

    @Override
    public Uni<Response> dump(String arg0) {
        return redisAPI.dump(arg0);
    }

    @Override
    public Response dumpAndAwait(String arg0) {
        return redisAPI.dumpAndAwait(arg0);
    }

    @Override
    public Uni<Response> echo(String arg0) {
        return redisAPI.echo(arg0);
    }

    @Override
    public Response echoAndAwait(String arg0) {
        return redisAPI.echoAndAwait(arg0);
    }

    @Override
    public Uni<Response> eval(List<String> args) {
        return redisAPI.eval(args);
    }

    @Override
    public Response evalAndAwait(List<String> args) {
        return redisAPI.evalAndAwait(args);
    }

    @Override
    public Uni<Response> evalsha(List<String> args) {
        return redisAPI.evalsha(args);
    }

    @Override
    public Response evalshaAndAwait(List<String> args) {
        return redisAPI.evalshaAndAwait(args);
    }

    @Override
    public Uni<Response> exec() {
        return redisAPI.exec();
    }

    @Override
    public Response execAndAwait() {
        return redisAPI.execAndAwait();
    }

    @Override
    public Uni<Response> exists(List<String> args) {
        return redisAPI.exists(args);
    }

    @Override
    public Response existsAndAwait(List<String> args) {
        return redisAPI.existsAndAwait(args);
    }

    @Override
    public Uni<Response> expire(String arg0, String arg1) {
        return redisAPI.expire(arg0, arg1);
    }

    @Override
    public Response expireAndAwait(String arg0, String arg1) {
        return redisAPI.expireAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> expireat(String arg0, String arg1) {
        return redisAPI.expireat(arg0, arg1);
    }

    @Override
    public Response expireatAndAwait(String arg0, String arg1) {
        return redisAPI.expireatAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> flushall(List<String> args) {
        return redisAPI.flushall(args);
    }

    @Override
    public Response flushallAndAwait(List<String> args) {
        return redisAPI.flushallAndAwait(args);
    }

    @Override
    public Uni<Response> flushdb(List<String> args) {
        return redisAPI.flushdb(args);
    }

    @Override
    public Response flushdbAndAwait(List<String> args) {
        return redisAPI.flushdbAndAwait(args);
    }

    @Override
    public Uni<Response> geoadd(List<String> args) {
        return redisAPI.geoadd(args);
    }

    @Override
    public Response geoaddAndAwait(List<String> args) {
        return redisAPI.geoaddAndAwait(args);
    }

    @Override
    public Uni<Response> geodist(List<String> args) {
        return redisAPI.geodist(args);
    }

    @Override
    public Response geodistAndAwait(List<String> args) {
        return redisAPI.geodistAndAwait(args);
    }

    @Override
    public Uni<Response> geohash(List<String> args) {
        return redisAPI.geohash(args);
    }

    @Override
    public Response geohashAndAwait(List<String> args) {
        return redisAPI.geohashAndAwait(args);
    }

    @Override
    public Uni<Response> geopos(List<String> args) {
        return redisAPI.geopos(args);
    }

    @Override
    public Response geoposAndAwait(List<String> args) {
        return redisAPI.geoposAndAwait(args);
    }

    @Override
    public Uni<Response> georadius(List<String> args) {
        return redisAPI.georadius(args);
    }

    @Override
    public Response georadiusAndAwait(List<String> args) {
        return redisAPI.georadiusAndAwait(args);
    }

    @Override
    public Uni<Response> georadiusRo(List<String> args) {
        return redisAPI.georadiusRo(args);
    }

    @Override
    public Response georadiusRoAndAwait(List<String> args) {
        return redisAPI.georadiusRoAndAwait(args);
    }

    @Override
    public Uni<Response> georadiusbymember(List<String> args) {
        return redisAPI.georadiusbymember(args);
    }

    @Override
    public Response georadiusbymemberAndAwait(List<String> args) {
        return redisAPI.georadiusbymemberAndAwait(args);
    }

    @Override
    public Uni<Response> georadiusbymemberRo(List<String> args) {
        return redisAPI.georadiusbymemberRo(args);
    }

    @Override
    public Response georadiusbymemberRoAndAwait(List<String> args) {
        return redisAPI.georadiusbymemberRoAndAwait(args);
    }

    @Override
    public Uni<Response> get(String arg0) {
        return redisAPI.get(arg0);
    }

    @Override
    public Response getAndAwait(String arg0) {
        return redisAPI.getAndAwait(arg0);
    }

    @Override
    public Uni<Response> getbit(String arg0, String arg1) {
        return redisAPI.getbit(arg0, arg1);
    }

    @Override
    public Response getbitAndAwait(String arg0, String arg1) {
        return redisAPI.getbitAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> getrange(String arg0, String arg1, String arg2) {
        return redisAPI.getrange(arg0, arg1, arg2);
    }

    @Override
    public Response getrangeAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.getrangeAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> getset(String arg0, String arg1) {
        return redisAPI.getset(arg0, arg1);
    }

    @Override
    public Response getsetAndAwait(String arg0, String arg1) {
        return redisAPI.getsetAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> hdel(List<String> args) {
        return redisAPI.hdel(args);
    }

    @Override
    public Response hdelAndAwait(List<String> args) {
        return redisAPI.hdelAndAwait(args);
    }

    @Override
    public Uni<Response> hexists(String arg0, String arg1) {
        return redisAPI.hexists(arg0, arg1);
    }

    @Override
    public Response hexistsAndAwait(String arg0, String arg1) {
        return redisAPI.hexistsAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> hget(String arg0, String arg1) {
        return redisAPI.hget(arg0, arg1);
    }

    @Override
    public Response hgetAndAwait(String arg0, String arg1) {
        return redisAPI.hgetAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> hgetall(String arg0) {
        return redisAPI.hgetall(arg0);
    }

    @Override
    public Response hgetallAndAwait(String arg0) {
        return redisAPI.hgetallAndAwait(arg0);
    }

    @Override
    public Uni<Response> hincrby(String arg0, String arg1, String arg2) {
        return redisAPI.hincrby(arg0, arg1, arg2);
    }

    @Override
    public Response hincrbyAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.hincrbyAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> hincrbyfloat(String arg0, String arg1, String arg2) {
        return redisAPI.hincrbyfloat(arg0, arg1, arg2);
    }

    @Override
    public Response hincrbyfloatAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.hincrbyfloatAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> hkeys(String arg0) {
        return redisAPI.hkeys(arg0);
    }

    @Override
    public Response hkeysAndAwait(String arg0) {
        return redisAPI.hkeysAndAwait(arg0);
    }

    @Override
    public Uni<Response> hlen(String arg0) {
        return redisAPI.hlen(arg0);
    }

    @Override
    public Response hlenAndAwait(String arg0) {
        return redisAPI.hlenAndAwait(arg0);
    }

    @Override
    public Uni<Response> hmget(List<String> args) {
        return redisAPI.hmget(args);
    }

    @Override
    public Response hmgetAndAwait(List<String> args) {
        return redisAPI.hmgetAndAwait(args);
    }

    @Override
    public Uni<Response> hmset(List<String> args) {
        return redisAPI.hmset(args);
    }

    @Override
    public Response hmsetAndAwait(List<String> args) {
        return redisAPI.hmsetAndAwait(args);
    }

    @Override
    public Uni<Response> host(List<String> args) {
        return redisAPI.host(args);
    }

    @Override
    public Response hostAndAwait(List<String> args) {
        return redisAPI.hostAndAwait(args);
    }

    @Override
    public Uni<Response> hscan(List<String> args) {
        return redisAPI.hscan(args);
    }

    @Override
    public Response hscanAndAwait(List<String> args) {
        return redisAPI.hscanAndAwait(args);
    }

    @Override
    public Uni<Response> hset(List<String> args) {
        return redisAPI.hset(args);
    }

    @Override
    public Response hsetAndAwait(List<String> args) {
        return redisAPI.hsetAndAwait(args);
    }

    @Override
    public Uni<Response> hsetnx(String arg0, String arg1, String arg2) {
        return redisAPI.hsetnx(arg0, arg1, arg2);
    }

    @Override
    public Response hsetnxAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.hsetnxAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> hstrlen(String arg0, String arg1) {
        return redisAPI.hstrlen(arg0, arg1);
    }

    @Override
    public Response hstrlenAndAwait(String arg0, String arg1) {
        return redisAPI.hstrlenAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> hvals(String arg0) {
        return redisAPI.hvals(arg0);
    }

    @Override
    public Response hvalsAndAwait(String arg0) {
        return redisAPI.hvalsAndAwait(arg0);
    }

    @Override
    public Uni<Response> incr(String arg0) {
        return redisAPI.incr(arg0);
    }

    @Override
    public Response incrAndAwait(String arg0) {
        return redisAPI.incrAndAwait(arg0);
    }

    @Override
    public Uni<Response> incrby(String arg0, String arg1) {
        return redisAPI.incrby(arg0, arg1);
    }

    @Override
    public Response incrbyAndAwait(String arg0, String arg1) {
        return redisAPI.incrbyAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> incrbyfloat(String arg0, String arg1) {
        return redisAPI.incrbyfloat(arg0, arg1);
    }

    @Override
    public Response incrbyfloatAndAwait(String arg0, String arg1) {
        return redisAPI.incrbyfloatAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> info(List<String> args) {
        return redisAPI.info(args);
    }

    @Override
    public Response infoAndAwait(List<String> args) {
        return redisAPI.infoAndAwait(args);
    }

    @Override
    public Uni<Response> keys(String arg0) {
        return redisAPI.keys(arg0);
    }

    @Override
    public Response keysAndAwait(String arg0) {
        return redisAPI.keysAndAwait(arg0);
    }

    @Override
    public Uni<Response> lastsave() {
        return redisAPI.lastsave();
    }

    @Override
    public Response lastsaveAndAwait() {
        return redisAPI.lastsaveAndAwait();
    }

    @Override
    public Uni<Response> latency(List<String> args) {
        return redisAPI.latency(args);
    }

    @Override
    public Response latencyAndAwait(List<String> args) {
        return redisAPI.latencyAndAwait(args);
    }

    @Override
    public Uni<Response> lindex(String arg0, String arg1) {
        return redisAPI.lindex(arg0, arg1);
    }

    @Override
    public Response lindexAndAwait(String arg0, String arg1) {
        return redisAPI.lindexAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> linsert(String arg0, String arg1, String arg2, String arg3) {
        return redisAPI.linsert(arg0, arg1, arg2, arg3);
    }

    @Override
    public Response linsertAndAwait(String arg0, String arg1, String arg2, String arg3) {
        return redisAPI.linsertAndAwait(arg0, arg1, arg2, arg3);
    }

    @Override
    public Uni<Response> llen(String arg0) {
        return redisAPI.llen(arg0);
    }

    @Override
    public Response llenAndAwait(String arg0) {
        return redisAPI.llenAndAwait(arg0);
    }

    @Override
    public Uni<Response> lolwut(List<String> args) {
        return redisAPI.lolwut(args);
    }

    @Override
    public Response lolwutAndAwait(List<String> args) {
        return redisAPI.lolwutAndAwait(args);
    }

    @Override
    public Uni<Response> lpop(String arg0) {
        return redisAPI.lpop(List.of(arg0));
    }

    @Override
    public Uni<Response> lpop(List<String> arg0) {
        return redisAPI.lpop(arg0);
    }

    @Override
    public Response lpopAndAwait(String arg0) {
        return redisAPI.lpopAndAwait(List.of(arg0));
    }

    @Override
    public Response lpopAndAwait(List<String> arg0) {
        return redisAPI.lpopAndAwait(arg0);
    }

    @Override
    public Uni<Response> lpush(List<String> args) {
        return redisAPI.lpush(args);
    }

    @Override
    public Response lpushAndAwait(List<String> args) {
        return redisAPI.lpushAndAwait(args);
    }

    @Override
    public Uni<Response> lpushx(List<String> args) {
        return redisAPI.lpushx(args);
    }

    @Override
    public Response lpushxAndAwait(List<String> args) {
        return redisAPI.lpushxAndAwait(args);
    }

    @Override
    public Uni<Response> lrange(String arg0, String arg1, String arg2) {
        return redisAPI.lrange(arg0, arg1, arg2);
    }

    @Override
    public Response lrangeAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.lrangeAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> lrem(String arg0, String arg1, String arg2) {
        return redisAPI.lrem(arg0, arg1, arg2);
    }

    @Override
    public Response lremAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.lremAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> lset(String arg0, String arg1, String arg2) {
        return redisAPI.lset(arg0, arg1, arg2);
    }

    @Override
    public Response lsetAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.lsetAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> ltrim(String arg0, String arg1, String arg2) {
        return redisAPI.ltrim(arg0, arg1, arg2);
    }

    @Override
    public Response ltrimAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.ltrimAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> memory(List<String> args) {
        return redisAPI.memory(args);
    }

    @Override
    public Response memoryAndAwait(List<String> args) {
        return redisAPI.memoryAndAwait(args);
    }

    @Override
    public Uni<Response> mget(List<String> args) {
        return redisAPI.mget(args);
    }

    @Override
    public Response mgetAndAwait(List<String> args) {
        return redisAPI.mgetAndAwait(args);
    }

    @Override
    public Uni<Response> migrate(List<String> args) {
        return redisAPI.migrate(args);
    }

    @Override
    public Response migrateAndAwait(List<String> args) {
        return redisAPI.migrateAndAwait(args);
    }

    @Override
    public Uni<Response> module(List<String> args) {
        return redisAPI.module(args);
    }

    @Override
    public Response moduleAndAwait(List<String> args) {
        return redisAPI.moduleAndAwait(args);
    }

    @Override
    public Uni<Response> monitor() {
        return redisAPI.monitor();
    }

    @Override
    public Response monitorAndAwait() {
        return redisAPI.monitorAndAwait();
    }

    @Override
    public Uni<Response> move(String arg0, String arg1) {
        return redisAPI.move(arg0, arg1);
    }

    @Override
    public Response moveAndAwait(String arg0, String arg1) {
        return redisAPI.moveAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> mset(List<String> args) {
        return redisAPI.mset(args);
    }

    @Override
    public Response msetAndAwait(List<String> args) {
        return redisAPI.msetAndAwait(args);
    }

    @Override
    public Uni<Response> msetnx(List<String> args) {
        return redisAPI.msetnx(args);
    }

    @Override
    public Response msetnxAndAwait(List<String> args) {
        return redisAPI.msetnxAndAwait(args);
    }

    @Override
    public Uni<Response> multi() {
        return redisAPI.multi();
    }

    @Override
    public Response multiAndAwait() {
        return redisAPI.multiAndAwait();
    }

    @Override
    public Uni<Response> object(List<String> args) {
        return redisAPI.object(args);
    }

    @Override
    public Response objectAndAwait(List<String> args) {
        return redisAPI.objectAndAwait(args);
    }

    @Override
    public Uni<Response> persist(String arg0) {
        return redisAPI.persist(arg0);
    }

    @Override
    public Response persistAndAwait(String arg0) {
        return redisAPI.persistAndAwait(arg0);
    }

    @Override
    public Uni<Response> pexpire(String arg0, String arg1) {
        return redisAPI.pexpire(arg0, arg1);
    }

    @Override
    public Response pexpireAndAwait(String arg0, String arg1) {
        return redisAPI.pexpireAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> pexpireat(String arg0, String arg1) {
        return redisAPI.pexpireat(arg0, arg1);
    }

    @Override
    public Response pexpireatAndAwait(String arg0, String arg1) {
        return redisAPI.pexpireatAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> pfadd(List<String> args) {
        return redisAPI.pfadd(args);
    }

    @Override
    public Response pfaddAndAwait(List<String> args) {
        return redisAPI.pfaddAndAwait(args);
    }

    @Override
    public Uni<Response> pfcount(List<String> args) {
        return redisAPI.pfcount(args);
    }

    @Override
    public Response pfcountAndAwait(List<String> args) {
        return redisAPI.pfcountAndAwait(args);
    }

    @Override
    public Uni<Response> pfdebug(List<String> args) {
        return redisAPI.pfdebug(args);
    }

    @Override
    public Response pfdebugAndAwait(List<String> args) {
        return redisAPI.pfdebugAndAwait(args);
    }

    @Override
    public Uni<Response> pfmerge(List<String> args) {
        return redisAPI.pfmerge(args);
    }

    @Override
    public Response pfmergeAndAwait(List<String> args) {
        return redisAPI.pfmergeAndAwait(args);
    }

    @Override
    public Uni<Response> pfselftest() {
        return redisAPI.pfselftest();
    }

    @Override
    public Response pfselftestAndAwait() {
        return redisAPI.pfselftestAndAwait();
    }

    @Override
    public Uni<Response> ping(List<String> args) {
        return redisAPI.ping(args);
    }

    @Override
    public Response pingAndAwait(List<String> args) {
        return redisAPI.pingAndAwait(args);
    }

    @Override
    public Uni<Response> post(List<String> args) {
        return redisAPI.post(args);
    }

    @Override
    public Response postAndAwait(List<String> args) {
        return redisAPI.postAndAwait(args);
    }

    @Override
    public Uni<Response> psetex(String arg0, String arg1, String arg2) {
        return redisAPI.psetex(arg0, arg1, arg2);
    }

    @Override
    public Response psetexAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.psetexAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> psubscribe(List<String> args) {
        return redisAPI.psubscribe(args);
    }

    @Override
    public Response psubscribeAndAwait(List<String> args) {
        return redisAPI.psubscribeAndAwait(args);
    }

    @Override
    public Uni<Response> psync(String arg0, String arg1) {
        return redisAPI.psync(List.of(arg0, arg1));
    }

    @Override
    public Uni<Response> psync(List<String> args) {
        return redisAPI.psync(args);
    }

    @Override
    public Response psyncAndAwait(List<String> args) {
        return redisAPI.psyncAndAwait(args);
    }

    @Override
    public Response psyncAndAwait(String arg0, String arg1) {
        return redisAPI.psyncAndAwait(List.of(arg0, arg1));
    }

    @Override
    public Uni<Response> pttl(String arg0) {
        return redisAPI.pttl(arg0);
    }

    @Override
    public Response pttlAndAwait(String arg0) {
        return redisAPI.pttlAndAwait(arg0);
    }

    @Override
    public Uni<Response> publish(String arg0, String arg1) {
        return redisAPI.publish(arg0, arg1);
    }

    @Override
    public Response publishAndAwait(String arg0, String arg1) {
        return redisAPI.publishAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> pubsub(List<String> args) {
        return redisAPI.pubsub(args);
    }

    @Override
    public Response pubsubAndAwait(List<String> args) {
        return redisAPI.pubsubAndAwait(args);
    }

    @Override
    public Uni<Response> punsubscribe(List<String> args) {
        return redisAPI.punsubscribe(args);
    }

    @Override
    public Response punsubscribeAndAwait(List<String> args) {
        return redisAPI.punsubscribeAndAwait(args);
    }

    @Override
    public Uni<Response> randomkey() {
        return redisAPI.randomkey();
    }

    @Override
    public Response randomkeyAndAwait() {
        return redisAPI.randomkeyAndAwait();
    }

    @Override
    public Uni<Response> readonly() {
        return redisAPI.readonly();
    }

    @Override
    public Response readonlyAndAwait() {
        return redisAPI.readonlyAndAwait();
    }

    @Override
    public Uni<Response> readwrite() {
        return redisAPI.readwrite();
    }

    @Override
    public Response readwriteAndAwait() {
        return redisAPI.readwriteAndAwait();
    }

    @Override
    public Uni<Response> rename(String arg0, String arg1) {
        return redisAPI.rename(arg0, arg1);
    }

    @Override
    public Response renameAndAwait(String arg0, String arg1) {
        return redisAPI.renameAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> renamenx(String arg0, String arg1) {
        return redisAPI.renamenx(arg0, arg1);
    }

    @Override
    public Response renamenxAndAwait(String arg0, String arg1) {
        return redisAPI.renamenxAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> replconf(List<String> args) {
        return redisAPI.replconf(args);
    }

    @Override
    public Response replconfAndAwait(List<String> args) {
        return redisAPI.replconfAndAwait(args);
    }

    @Override
    public Uni<Response> replicaof(String arg0, String arg1) {
        return redisAPI.replicaof(arg0, arg1);
    }

    @Override
    public Response replicaofAndAwait(String arg0, String arg1) {
        return redisAPI.replicaofAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> restore(List<String> args) {
        return redisAPI.restore(args);
    }

    @Override
    public Response restoreAndAwait(List<String> args) {
        return redisAPI.restoreAndAwait(args);
    }

    @Override
    public Uni<Response> restoreAsking(List<String> args) {
        return redisAPI.restoreAsking(args);
    }

    @Override
    public Response restoreAskingAndAwait(List<String> args) {
        return redisAPI.restoreAskingAndAwait(args);
    }

    @Override
    public Uni<Response> role() {
        return redisAPI.role();
    }

    @Override
    public Response roleAndAwait() {
        return redisAPI.roleAndAwait();
    }

    @Override
    public Uni<Response> rpop(String arg0) {
        return redisAPI.rpop(List.of(arg0));
    }

    @Override
    public Uni<Response> rpop(List<String> args) {
        return redisAPI.rpop(args);
    }

    @Override
    public Response rpopAndAwait(List<String> args) {
        return redisAPI.rpopAndAwait(args);
    }

    @Override
    public Response rpopAndAwait(String arg0) {
        return redisAPI.rpopAndAwait(List.of(arg0));
    }

    @Override
    public Uni<Response> rpoplpush(String arg0, String arg1) {
        return redisAPI.rpoplpush(arg0, arg1);
    }

    @Override
    public Response rpoplpushAndAwait(String arg0, String arg1) {
        return redisAPI.rpoplpushAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> rpush(List<String> args) {
        return redisAPI.rpush(args);
    }

    @Override
    public Response rpushAndAwait(List<String> args) {
        return redisAPI.rpushAndAwait(args);
    }

    @Override
    public Uni<Response> rpushx(List<String> args) {
        return redisAPI.rpushx(args);
    }

    @Override
    public Response rpushxAndAwait(List<String> args) {
        return redisAPI.rpushxAndAwait(args);
    }

    @Override
    public Uni<Response> sadd(List<String> args) {
        return redisAPI.sadd(args);
    }

    @Override
    public Response saddAndAwait(List<String> args) {
        return redisAPI.saddAndAwait(args);
    }

    @Override
    public Uni<Response> save() {
        return redisAPI.save();
    }

    @Override
    public Response saveAndAwait() {
        return redisAPI.saveAndAwait();
    }

    @Override
    public Uni<Response> scan(List<String> args) {
        return redisAPI.scan(args);
    }

    @Override
    public Response scanAndAwait(List<String> args) {
        return redisAPI.scanAndAwait(args);
    }

    @Override
    public Uni<Response> scard(String arg0) {
        return redisAPI.scard(arg0);
    }

    @Override
    public Response scardAndAwait(String arg0) {
        return redisAPI.scardAndAwait(arg0);
    }

    @Override
    public Uni<Response> script(List<String> args) {
        return redisAPI.script(args);
    }

    @Override
    public Response scriptAndAwait(List<String> args) {
        return redisAPI.scriptAndAwait(args);
    }

    @Override
    public Uni<Response> sdiff(List<String> args) {
        return redisAPI.sdiff(args);
    }

    @Override
    public Response sdiffAndAwait(List<String> args) {
        return redisAPI.sdiffAndAwait(args);
    }

    @Override
    public Uni<Response> sdiffstore(List<String> args) {
        return redisAPI.sdiffstore(args);
    }

    @Override
    public Response sdiffstoreAndAwait(List<String> args) {
        return redisAPI.sdiffstoreAndAwait(args);
    }

    @Override
    public Uni<Response> select(String arg0) {
        return redisAPI.select(arg0);
    }

    @Override
    public Response selectAndAwait(String arg0) {
        return redisAPI.selectAndAwait(arg0);
    }

    @Override
    public Uni<Response> set(List<String> args) {
        return redisAPI.set(args);
    }

    @Override
    public Response setAndAwait(List<String> args) {
        return redisAPI.setAndAwait(args);
    }

    @Override
    public Uni<Response> setbit(String arg0, String arg1, String arg2) {
        return redisAPI.setbit(arg0, arg1, arg2);
    }

    @Override
    public Response setbitAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.setbitAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> setex(String arg0, String arg1, String arg2) {
        return redisAPI.setex(arg0, arg1, arg2);
    }

    @Override
    public Response setexAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.setexAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> setnx(String arg0, String arg1) {
        return redisAPI.setnx(arg0, arg1);
    }

    @Override
    public Response setnxAndAwait(String arg0, String arg1) {
        return redisAPI.setnxAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> setrange(String arg0, String arg1, String arg2) {
        return redisAPI.setrange(arg0, arg1, arg2);
    }

    @Override
    public Response setrangeAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.setrangeAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> shutdown(List<String> args) {
        return redisAPI.shutdown(args);
    }

    @Override
    public Response shutdownAndAwait(List<String> args) {
        return redisAPI.shutdownAndAwait(args);
    }

    @Override
    public Uni<Response> sinter(List<String> args) {
        return redisAPI.sinter(args);
    }

    @Override
    public Response sinterAndAwait(List<String> args) {
        return redisAPI.sinterAndAwait(args);
    }

    @Override
    public Uni<Response> sinterstore(List<String> args) {
        return redisAPI.sinterstore(args);
    }

    @Override
    public Response sinterstoreAndAwait(List<String> args) {
        return redisAPI.sinterstoreAndAwait(args);
    }

    @Override
    public Uni<Response> sismember(String arg0, String arg1) {
        return redisAPI.sismember(arg0, arg1);
    }

    @Override
    public Response sismemberAndAwait(String arg0, String arg1) {
        return redisAPI.sismemberAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> slaveof(String arg0, String arg1) {
        return redisAPI.slaveof(arg0, arg1);
    }

    @Override
    public Response slaveofAndAwait(String arg0, String arg1) {
        return redisAPI.slaveofAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> slowlog(List<String> args) {
        return redisAPI.slowlog(args);
    }

    @Override
    public Response slowlogAndAwait(List<String> args) {
        return redisAPI.slowlogAndAwait(args);
    }

    @Override
    public Uni<Response> smembers(String arg0) {
        return redisAPI.smembers(arg0);
    }

    @Override
    public Response smembersAndAwait(String arg0) {
        return redisAPI.smembersAndAwait(arg0);
    }

    @Override
    public Uni<Response> smove(String arg0, String arg1, String arg2) {
        return redisAPI.smove(arg0, arg1, arg2);
    }

    @Override
    public Response smoveAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.smoveAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> sort(List<String> args) {
        return redisAPI.sort(args);
    }

    @Override
    public Response sortAndAwait(List<String> args) {
        return redisAPI.sortAndAwait(args);
    }

    @Override
    public Uni<Response> spop(List<String> args) {
        return redisAPI.spop(args);
    }

    @Override
    public Response spopAndAwait(List<String> args) {
        return redisAPI.spopAndAwait(args);
    }

    @Override
    public Uni<Response> srandmember(List<String> args) {
        return redisAPI.srandmember(args);
    }

    @Override
    public Response srandmemberAndAwait(List<String> args) {
        return redisAPI.srandmemberAndAwait(args);
    }

    @Override
    public Uni<Response> srem(List<String> args) {
        return redisAPI.srem(args);
    }

    @Override
    public Response sremAndAwait(List<String> args) {
        return redisAPI.sremAndAwait(args);
    }

    @Override
    public Uni<Response> sscan(List<String> args) {
        return redisAPI.sscan(args);
    }

    @Override
    public Response sscanAndAwait(List<String> args) {
        return redisAPI.sscanAndAwait(args);
    }

    @Override
    public Uni<Response> strlen(String arg0) {
        return redisAPI.strlen(arg0);
    }

    @Override
    public Response strlenAndAwait(String arg0) {
        return redisAPI.strlenAndAwait(arg0);
    }

    @Override
    public Uni<Response> subscribe(List<String> args) {
        return redisAPI.subscribe(args);
    }

    @Override
    public Response subscribeAndAwait(List<String> args) {
        return redisAPI.subscribeAndAwait(args);
    }

    @Override
    public Uni<Response> substr(String arg0, String arg1, String arg2) {
        return redisAPI.substr(arg0, arg1, arg2);
    }

    @Override
    public Response substrAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.substrAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> sunion(List<String> args) {
        return redisAPI.sunion(args);
    }

    @Override
    public Response sunionAndAwait(List<String> args) {
        return redisAPI.sunionAndAwait(args);
    }

    @Override
    public Uni<Response> sunionstore(List<String> args) {
        return redisAPI.sunionstore(args);
    }

    @Override
    public Response sunionstoreAndAwait(List<String> args) {
        return redisAPI.sunionstoreAndAwait(args);
    }

    @Override
    public Uni<Response> swapdb(String arg0, String arg1) {
        return redisAPI.swapdb(arg0, arg1);
    }

    @Override
    public Response swapdbAndAwait(String arg0, String arg1) {
        return redisAPI.swapdbAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> sync() {
        return redisAPI.sync();
    }

    @Override
    public Response syncAndAwait() {
        return redisAPI.syncAndAwait();
    }

    @Override
    public Uni<Response> time() {
        return redisAPI.time();
    }

    @Override
    public Response timeAndAwait() {
        return redisAPI.timeAndAwait();
    }

    @Override
    public Uni<Response> touch(List<String> args) {
        return redisAPI.touch(args);
    }

    @Override
    public Response touchAndAwait(List<String> args) {
        return redisAPI.touchAndAwait(args);
    }

    @Override
    public Uni<Response> ttl(String arg0) {
        return redisAPI.ttl(arg0);
    }

    @Override
    public Response ttlAndAwait(String arg0) {
        return redisAPI.ttlAndAwait(arg0);
    }

    @Override
    public Uni<Response> type(String arg0) {
        return redisAPI.type(arg0);
    }

    @Override
    public Response typeAndAwait(String arg0) {
        return redisAPI.typeAndAwait(arg0);
    }

    @Override
    public Uni<Response> unlink(List<String> args) {
        return redisAPI.unlink(args);
    }

    @Override
    public Response unlinkAndAwait(List<String> args) {
        return redisAPI.unlinkAndAwait(args);
    }

    @Override
    public Uni<Response> unsubscribe(List<String> args) {
        return redisAPI.unsubscribe(args);
    }

    @Override
    public Response unsubscribeAndAwait(List<String> args) {
        return redisAPI.unsubscribeAndAwait(args);
    }

    @Override
    public Uni<Response> unwatch() {
        return redisAPI.unwatch();
    }

    @Override
    public Response unwatchAndAwait() {
        return redisAPI.unwatchAndAwait();
    }

    @Override
    public Uni<Response> wait(String arg0, String arg1) {
        return redisAPI.wait(arg0, arg1);
    }

    @Override
    public Response waitAndAwait(String arg0, String arg1) {
        return redisAPI.waitAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> watch(List<String> args) {
        return redisAPI.watch(args);
    }

    @Override
    public Response watchAndAwait(List<String> args) {
        return redisAPI.watchAndAwait(args);
    }

    @Override
    public Uni<Response> xack(List<String> args) {
        return redisAPI.xack(args);
    }

    @Override
    public Response xackAndAwait(List<String> args) {
        return redisAPI.xackAndAwait(args);
    }

    @Override
    public Uni<Response> xadd(List<String> args) {
        return redisAPI.xadd(args);
    }

    @Override
    public Response xaddAndAwait(List<String> args) {
        return redisAPI.xaddAndAwait(args);
    }

    @Override
    public Uni<Response> xclaim(List<String> args) {
        return redisAPI.xclaim(args);
    }

    @Override
    public Response xclaimAndAwait(List<String> args) {
        return redisAPI.xclaimAndAwait(args);
    }

    @Override
    public Uni<Response> xdel(List<String> args) {
        return redisAPI.xdel(args);
    }

    @Override
    public Response xdelAndAwait(List<String> args) {
        return redisAPI.xdelAndAwait(args);
    }

    @Override
    public Uni<Response> xgroup(List<String> args) {
        return redisAPI.xgroup(args);
    }

    @Override
    public Response xgroupAndAwait(List<String> args) {
        return redisAPI.xgroupAndAwait(args);
    }

    @Override
    public Uni<Response> xinfo(List<String> args) {
        return redisAPI.xinfo(args);
    }

    @Override
    public Response xinfoAndAwait(List<String> args) {
        return redisAPI.xinfoAndAwait(args);
    }

    @Override
    public Uni<Response> xlen(String arg0) {
        return redisAPI.xlen(arg0);
    }

    @Override
    public Response xlenAndAwait(String arg0) {
        return redisAPI.xlenAndAwait(arg0);
    }

    @Override
    public Uni<Response> xpending(List<String> args) {
        return redisAPI.xpending(args);
    }

    @Override
    public Response xpendingAndAwait(List<String> args) {
        return redisAPI.xpendingAndAwait(args);
    }

    @Override
    public Uni<Response> xrange(List<String> args) {
        return redisAPI.xrange(args);
    }

    @Override
    public Response xrangeAndAwait(List<String> args) {
        return redisAPI.xrangeAndAwait(args);
    }

    @Override
    public Uni<Response> xread(List<String> args) {
        return redisAPI.xread(args);
    }

    @Override
    public Response xreadAndAwait(List<String> args) {
        return redisAPI.xreadAndAwait(args);
    }

    @Override
    public Uni<Response> xreadgroup(List<String> args) {
        return redisAPI.xreadgroup(args);
    }

    @Override
    public Response xreadgroupAndAwait(List<String> args) {
        return redisAPI.xreadgroupAndAwait(args);
    }

    @Override
    public Uni<Response> xrevrange(List<String> args) {
        return redisAPI.xrevrange(args);
    }

    @Override
    public Response xrevrangeAndAwait(List<String> args) {
        return redisAPI.xrevrangeAndAwait(args);
    }

    @Override
    public Uni<Response> xsetid(String arg0, String arg1) {
        return redisAPI.xsetid(arg0, arg1);
    }

    @Override
    public Response xsetidAndAwait(String arg0, String arg1) {
        return redisAPI.xsetidAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> xtrim(List<String> args) {
        return redisAPI.xtrim(args);
    }

    @Override
    public Response xtrimAndAwait(List<String> args) {
        return redisAPI.xtrimAndAwait(args);
    }

    @Override
    public Uni<Response> zadd(List<String> args) {
        return redisAPI.zadd(args);
    }

    @Override
    public Response zaddAndAwait(List<String> args) {
        return redisAPI.zaddAndAwait(args);
    }

    @Override
    public Uni<Response> zcard(String arg0) {
        return redisAPI.zcard(arg0);
    }

    @Override
    public Response zcardAndAwait(String arg0) {
        return redisAPI.zcardAndAwait(arg0);
    }

    @Override
    public Uni<Response> zcount(String arg0, String arg1, String arg2) {
        return redisAPI.zcount(arg0, arg1, arg2);
    }

    @Override
    public Response zcountAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zcountAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zincrby(String arg0, String arg1, String arg2) {
        return redisAPI.zincrby(arg0, arg1, arg2);
    }

    @Override
    public Response zincrbyAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zincrbyAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zinterstore(List<String> args) {
        return redisAPI.zinterstore(args);
    }

    @Override
    public Response zinterstoreAndAwait(List<String> args) {
        return redisAPI.zinterstoreAndAwait(args);
    }

    @Override
    public Uni<Response> zlexcount(String arg0, String arg1, String arg2) {
        return redisAPI.zlexcount(arg0, arg1, arg2);
    }

    @Override
    public Response zlexcountAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zlexcountAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zpopmax(List<String> args) {
        return redisAPI.zpopmax(args);
    }

    @Override
    public Response zpopmaxAndAwait(List<String> args) {
        return redisAPI.zpopmaxAndAwait(args);
    }

    @Override
    public Uni<Response> zpopmin(List<String> args) {
        return redisAPI.zpopmin(args);
    }

    @Override
    public Response zpopminAndAwait(List<String> args) {
        return redisAPI.zpopminAndAwait(args);
    }

    @Override
    public Uni<Response> zrange(List<String> args) {
        return redisAPI.zrange(args);
    }

    @Override
    public Response zrangeAndAwait(List<String> args) {
        return redisAPI.zrangeAndAwait(args);
    }

    @Override
    public Uni<Response> zrangebylex(List<String> args) {
        return redisAPI.zrangebylex(args);
    }

    @Override
    public Response zrangebylexAndAwait(List<String> args) {
        return redisAPI.zrangebylexAndAwait(args);
    }

    @Override
    public Uni<Response> zrangebyscore(List<String> args) {
        return redisAPI.zrangebyscore(args);
    }

    @Override
    public Response zrangebyscoreAndAwait(List<String> args) {
        return redisAPI.zrangebyscoreAndAwait(args);
    }

    @Override
    public Uni<Response> zrank(String arg0, String arg1) {
        return redisAPI.zrank(arg0, arg1);
    }

    @Override
    public Response zrankAndAwait(String arg0, String arg1) {
        return redisAPI.zrankAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> zrem(List<String> args) {
        return redisAPI.zrem(args);
    }

    @Override
    public Response zremAndAwait(List<String> args) {
        return redisAPI.zremAndAwait(args);
    }

    @Override
    public Uni<Response> zremrangebylex(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebylex(arg0, arg1, arg2);
    }

    @Override
    public Response zremrangebylexAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebylexAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zremrangebyrank(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebyrank(arg0, arg1, arg2);
    }

    @Override
    public Response zremrangebyrankAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebyrankAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zremrangebyscore(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebyscore(arg0, arg1, arg2);
    }

    @Override
    public Response zremrangebyscoreAndAwait(String arg0, String arg1, String arg2) {
        return redisAPI.zremrangebyscoreAndAwait(arg0, arg1, arg2);
    }

    @Override
    public Uni<Response> zrevrange(List<String> args) {
        return redisAPI.zrevrange(args);
    }

    @Override
    public Response zrevrangeAndAwait(List<String> args) {
        return redisAPI.zrevrangeAndAwait(args);
    }

    @Override
    public Uni<Response> zrevrangebylex(List<String> args) {
        return redisAPI.zrevrangebylex(args);
    }

    @Override
    public Response zrevrangebylexAndAwait(List<String> args) {
        return redisAPI.zrevrangebylexAndAwait(args);
    }

    @Override
    public Uni<Response> zrevrangebyscore(List<String> args) {
        return redisAPI.zrevrangebyscore(args);
    }

    @Override
    public Response zrevrangebyscoreAndAwait(List<String> args) {
        return redisAPI.zrevrangebyscoreAndAwait(args);
    }

    @Override
    public Uni<Response> zrevrank(String arg0, String arg1) {
        return redisAPI.zrevrank(arg0, arg1);
    }

    @Override
    public Response zrevrankAndAwait(String arg0, String arg1) {
        return redisAPI.zrevrankAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> zscan(List<String> args) {
        return redisAPI.zscan(args);
    }

    @Override
    public Response zscanAndAwait(List<String> args) {
        return redisAPI.zscanAndAwait(args);
    }

    @Override
    public Uni<Response> zscore(String arg0, String arg1) {
        return redisAPI.zscore(arg0, arg1);
    }

    @Override
    public Response zscoreAndAwait(String arg0, String arg1) {
        return redisAPI.zscoreAndAwait(arg0, arg1);
    }

    @Override
    public Uni<Response> zunionstore(List<String> args) {
        return redisAPI.zunionstore(args);
    }

    @Override
    public Response zunionstoreAndAwait(List<String> args) {
        return redisAPI.zunionstoreAndAwait(args);
    }
}
