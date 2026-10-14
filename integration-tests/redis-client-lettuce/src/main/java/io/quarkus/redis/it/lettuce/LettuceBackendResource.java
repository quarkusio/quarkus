package io.quarkus.redis.it.lettuce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.GeoValue;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.AggregationResponse;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.Document;
import io.quarkus.redis.datasource.search.FieldOptions;
import io.quarkus.redis.datasource.search.FieldType;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.search.SearchQueryResponse;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.search.SpellCheckResponse;
import io.quarkus.redis.datasource.search.SynDumpResponse;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.set.SetCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ScoreRange;
import io.quarkus.redis.datasource.sortedset.ScoredValue;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import io.quarkus.redis.datasource.sortedset.ZRangeArgs;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Response;

@Path("/lettuce")
@ApplicationScoped
public class LettuceBackendResource {

    private final RedisDataSource blocking;
    private final ReactiveRedisDataSource reactive;
    private final RedisDataSource secure;
    private final RedisDataSource cluster;
    private final RedisClusterClient clusterClient;
    private final RedisDataSource sentinel;
    private final RedisDataSource replication;
    private final ValueCommands<String, String> values;
    private final ReactiveValueCommands<String, String> reactiveValues;
    private final KeyCommands<String> keys;
    private final ReactiveKeyCommands<String> reactiveKeys;
    private final HashCommands<String, String, String> hash;
    private final ReactiveHashCommands<String, String, String> reactiveHash;
    private final ListCommands<String, String> list;
    private final ReactiveListCommands<String, String> reactiveList;
    private final SetCommands<String, String> set;
    private final ReactiveSetCommands<String, String> reactiveSet;
    private final SortedSetCommands<String, String> sortedSet;
    private final ReactiveSortedSetCommands<String, String> reactiveSortedSet;
    private final BitMapCommands<String> bitmap;
    private final ReactiveBitMapCommands<String> reactiveBitmap;
    private final HyperLogLogCommands<String, String> hyperloglog;
    private final ReactiveHyperLogLogCommands<String, String> reactiveHyperLogLog;
    private final GeoCommands<String, String> geo;
    private final ReactiveGeoCommands<String, String> reactiveGeo;
    private final JsonCommands<String> json;
    private final ReactiveJsonCommands<String> reactiveJson;
    private final SearchCommands<String> search;
    private final ReactiveSearchCommands<String> reactiveSearch;
    private final AutoSuggestCommands<String> autosuggest;
    private final ReactiveAutoSuggestCommands<String> reactiveAutoSuggest;
    private final CountMinCommands<String, String> countmin;
    private final ReactiveCountMinCommands<String, String> reactiveCountMin;

    @Inject
    public LettuceBackendResource(RedisDataSource ds, ReactiveRedisDataSource reactiveDs,
            @RedisClientName("secure") RedisDataSource secureDs,
            @RedisClientName("cluster") RedisDataSource clusterDs,
            @RedisClientName("cluster") RedisClusterClient clusterClient,
            @RedisClientName("sentinel") RedisDataSource sentinelDs,
            @RedisClientName("replication") RedisDataSource replicationDs) {
        this.blocking = ds;
        this.reactive = reactiveDs;
        this.secure = secureDs;
        this.cluster = clusterDs;
        this.clusterClient = clusterClient;
        this.sentinel = sentinelDs;
        this.replication = replicationDs;
        this.values = ds.value(String.class);
        this.reactiveValues = reactiveDs.value(String.class);
        this.keys = ds.key(String.class);
        this.reactiveKeys = reactiveDs.key(String.class);
        this.hash = ds.hash(String.class);
        this.reactiveHash = reactiveDs.hash(String.class);
        this.list = ds.list(String.class);
        this.reactiveList = reactiveDs.list(String.class);
        this.set = ds.set(String.class);
        this.reactiveSet = reactiveDs.set(String.class);
        this.sortedSet = ds.sortedSet(String.class);
        this.reactiveSortedSet = reactiveDs.sortedSet(String.class);
        this.bitmap = ds.bitmap(String.class);
        this.reactiveBitmap = reactiveDs.bitmap(String.class);
        this.hyperloglog = ds.hyperloglog(String.class);
        this.reactiveHyperLogLog = reactiveDs.hyperloglog(String.class);
        this.geo = ds.geo(String.class);
        this.reactiveGeo = reactiveDs.geo(String.class);
        this.json = ds.json();
        this.reactiveJson = reactiveDs.json();
        this.autosuggest = ds.autosuggest(String.class);
        this.reactiveAutoSuggest = reactiveDs.autosuggest(String.class);
        this.search = ds.search();
        this.reactiveSearch = reactiveDs.search();
        this.countmin = ds.countmin(String.class);
        this.reactiveCountMin = reactiveDs.countmin(String.class);
    }

    @GET
    @Path("/ping")
    public String ping() {
        Response response = blocking.execute("PING");
        return response.toString();
    }

    @GET
    @Path("/ping/command")
    public String pingCommand() {
        Response response = blocking.execute(Command.PING);
        return response.toString();
    }

    /**
     * Pings the {@code secure} client, connected over TLS (a {@code rediss://} URI, a PEM trust certificate and
     * hostname verification) to the TLS-only Redis server.
     */
    @GET
    @Path("/secure/ping")
    public String securePing() {
        Response response = secure.execute("PING");
        return response.toString();
    }

    /**
     * Pings the {@code cluster} client, connected to the six-node Redis cluster ({@code client-type=cluster}).
     */
    @GET
    @Path("/cluster/ping")
    public String clusterPing() {
        return cluster.execute("PING").toString();
    }

    /**
     * The topology the {@code cluster} client discovered from its three seed nodes: the number of upstream nodes and
     * of replicas.
     */
    @GET
    @Path("/cluster/topology")
    public String clusterTopology() {
        int upstream = 0;
        int replicas = 0;
        for (RedisClusterNode node : clusterClient.getPartitions()) {
            if (node.is(NodeFlag.UPSTREAM)) {
                upstream++;
            } else if (node.is(NodeFlag.REPLICA)) {
                replicas++;
            }
        }
        return upstream + "," + replicas;
    }

    @POST
    @Path("/cluster/value/{key}")
    public void clusterSetValue(@PathParam("key") String key, String value) {
        cluster.value(String.class).set(key, value);
    }

    @GET
    @Path("/cluster/value/{key}")
    public String clusterGetValue(@PathParam("key") String key) {
        return cluster.value(String.class).get(key);
    }

    /**
     * Scans the keys of every node of the cluster.
     */
    @GET
    @Path("/cluster/key/scan")
    public Set<String> clusterKeyScan(@QueryParam("match") String match) {
        KeyScanCursor<String> cursor = cluster.key(String.class).scan(new KeyScanArgs().match(match));
        Set<String> collected = new HashSet<>();
        while (cursor.hasNext()) {
            collected.addAll(cursor.next());
        }
        return collected;
    }

    /**
     * Transactions are not supported on a cluster: returns the message of the failure.
     */
    @POST
    @Path("/cluster/with-transaction/{key}")
    public String clusterWithTransaction(@PathParam("key") String key, String value) {
        try {
            cluster.withTransaction(tx -> tx.value(String.class, String.class).set(key, value));
            return "unexpected success";
        } catch (UnsupportedOperationException e) {
            return e.getMessage();
        }
    }

    /**
     * Pings the {@code sentinel} client, connected through the sentinels to the master they monitor
     * ({@code client-type=sentinel}).
     */
    @GET
    @Path("/sentinel/ping")
    public String sentinelPing() {
        return sentinel.execute("PING").toString();
    }

    /**
     * The role of the node the {@code sentinel} client talks to, from {@code INFO replication}: {@code master}.
     */
    @GET
    @Path("/sentinel/role")
    public String sentinelRole() {
        return role(sentinel);
    }

    @POST
    @Path("/sentinel/value/{key}")
    public void sentinelSetValue(@PathParam("key") String key, String value) {
        sentinel.value(String.class).set(key, value);
    }

    @GET
    @Path("/sentinel/value/{key}")
    public String sentinelGetValue(@PathParam("key") String key) {
        return sentinel.value(String.class).get(key);
    }

    /**
     * Pings the {@code replication} client, which discovered the master and its replica from the configured hosts
     * ({@code client-type=replication}).
     */
    @GET
    @Path("/replication/ping")
    public String replicationPing() {
        return replication.execute("PING").toString();
    }

    @GET
    @Path("/replication/role")
    public String replicationRole() {
        return role(replication);
    }

    @POST
    @Path("/replication/value/{key}")
    public void replicationSetValue(@PathParam("key") String key, String value) {
        replication.value(String.class).set(key, value);
    }

    @GET
    @Path("/replication/value/{key}")
    public String replicationGetValue(@PathParam("key") String key) {
        return replication.value(String.class).get(key);
    }

    private static String role(RedisDataSource ds) {
        String info = ds.execute("INFO", "replication").toString();
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("role:")) {
                return line.substring("role:".length()).trim();
            }
        }
        return "unknown";
    }

    /**
     * Tells which backend serves the data sources: only the Lettuce implementation rejects {@code getRedis()}.
     */
    @GET
    @Path("/backend")
    public String backend() {
        try {
            reactive.getRedis();
            return "vertx";
        } catch (UnsupportedOperationException e) {
            return "lettuce";
        }
    }

    @POST
    @Path("/value/{key}")
    public void setValue(@PathParam("key") String key, String value) {
        values.set(key, value);
    }

    @GET
    @Path("/value/{key}")
    public String getValue(@PathParam("key") String key) {
        return values.get(key);
    }

    @GET
    @Path("/value/lcs/{key1}/{key2}")
    public String lcs(@PathParam("key1") String key1, @PathParam("key2") String key2) {
        return values.lcs(key1, key2) + "," + values.lcsLength(key1, key2);
    }

    @POST
    @Path("/select/{index}")
    public void select(@PathParam("index") long index) {
        blocking.select(index);
    }

    @DELETE
    @Path("/flushall")
    public void flushall() {
        blocking.flushall();
    }

    @GET
    @Path("/reactive/{key}")
    public Uni<String> getReactive(@PathParam("key") String key) {
        return reactiveValues.get(key);
    }

    @GET
    @Path("/key/exists/{key}")
    public boolean keyExists(@PathParam("key") String key) {
        return keys.exists(key);
    }

    @DELETE
    @Path("/key/{key}")
    public int keyDel(@PathParam("key") String key) {
        return keys.del(key);
    }

    @POST
    @Path("/key/expire/{key}/{seconds}")
    public boolean keyExpire(@PathParam("key") String key, @PathParam("seconds") long seconds) {
        return keys.expire(key, seconds);
    }

    @GET
    @Path("/key/ttl/{key}")
    public long keyTtl(@PathParam("key") String key) {
        return keys.ttl(key);
    }

    @POST
    @Path("/key/persist/{key}")
    public boolean keyPersist(@PathParam("key") String key) {
        return keys.persist(key);
    }

    @POST
    @Path("/key/rename/{key}/{newkey}")
    public void keyRename(@PathParam("key") String key, @PathParam("newkey") String newkey) {
        keys.rename(key, newkey);
    }

    @POST
    @Path("/key/copy/{src}/{dst}")
    public boolean keyCopy(@PathParam("src") String src, @PathParam("dst") String dst) {
        return keys.copy(src, dst);
    }

    @GET
    @Path("/key/type/{key}")
    public String keyType(@PathParam("key") String key) {
        return keys.type(key).name();
    }

    @GET
    @Path("/key/scan")
    public Set<String> keyScan(@QueryParam("match") String match) {
        KeyScanArgs args = new KeyScanArgs();
        if (match != null) {
            args.match(match);
        }
        KeyScanCursor<String> cursor = keys.scan(args);
        Set<String> collected = new HashSet<>();
        while (cursor.hasNext()) {
            collected.addAll(cursor.next());
        }
        return collected;
    }

    @GET
    @Path("/key/reactive/ttl/{key}")
    public Uni<Long> keyTtlReactive(@PathParam("key") String key) {
        return reactiveKeys.ttl(key);
    }

    @POST
    @Path("/hash/{key}/{field}")
    public boolean hashSet(@PathParam("key") String key, @PathParam("field") String field, String value) {
        return hash.hset(key, field, value);
    }

    @GET
    @Path("/hash/{key}/{field}")
    public String hashGet(@PathParam("key") String key, @PathParam("field") String field) {
        return hash.hget(key, field);
    }

    @GET
    @Path("/hash/{key}")
    public Map<String, String> hashGetAll(@PathParam("key") String key) {
        return hash.hgetall(key);
    }

    @GET
    @Path("/hash/reactive/{key}/{field}")
    public Uni<String> hashGetReactive(@PathParam("key") String key, @PathParam("field") String field) {
        return reactiveHash.hget(key, field);
    }

    @POST
    @Path("/list/{key}")
    public long listPush(@PathParam("key") String key, String value) {
        return list.lpush(key, value);
    }

    @GET
    @Path("/list/{key}")
    public List<String> listRange(@PathParam("key") String key) {
        return list.lrange(key, 0, -1);
    }

    @GET
    @Path("/list/reactive/{key}")
    public Uni<List<String>> listRangeReactive(@PathParam("key") String key) {
        return reactiveList.lrange(key, 0, -1);
    }

    @POST
    @Path("/set/{key}")
    public int setAdd(@PathParam("key") String key, String value) {
        return set.sadd(key, value);
    }

    @GET
    @Path("/set/{key}")
    public Set<String> setMembers(@PathParam("key") String key) {
        return set.smembers(key);
    }

    @GET
    @Path("/set/ismember/{key}/{member}")
    public boolean setIsMember(@PathParam("key") String key, @PathParam("member") String member) {
        return set.sismember(key, member);
    }

    @GET
    @Path("/set/reactive/{key}")
    public Uni<Long> setCardReactive(@PathParam("key") String key) {
        return reactiveSet.scard(key);
    }

    @POST
    @Path("/sortedset/add/{key}/{score}")
    public boolean sortedSetAdd(@PathParam("key") String key, @PathParam("score") double score, String member) {
        return sortedSet.zadd(key, score, member);
    }

    @GET
    @Path("/sortedset/card/{key}")
    public long sortedSetCard(@PathParam("key") String key) {
        return sortedSet.zcard(key);
    }

    @GET
    @Path("/sortedset/score/{key}/{member}")
    public Double sortedSetScore(@PathParam("key") String key, @PathParam("member") String member) {
        OptionalDouble score = sortedSet.zscore(key, member);
        return score.isPresent() ? score.getAsDouble() : null;
    }

    @GET
    @Path("/sortedset/rank/{key}/{member}")
    public Long sortedSetRank(@PathParam("key") String key, @PathParam("member") String member) {
        OptionalLong rank = sortedSet.zrank(key, member);
        return rank.isPresent() ? rank.getAsLong() : null;
    }

    @POST
    @Path("/sortedset/popmin/{key}")
    public String sortedSetPopMin(@PathParam("key") String key) {
        ScoredValue<String> popped = sortedSet.zpopmin(key);
        return popped.value() + "," + popped.score();
    }

    @GET
    @Path("/sortedset/range/{key}/{start}/{stop}")
    public List<String> sortedSetRange(@PathParam("key") String key, @PathParam("start") long start,
            @PathParam("stop") long stop, @QueryParam("rev") boolean rev) {
        ZRangeArgs args = new ZRangeArgs();
        if (rev) {
            args.rev();
        }
        return sortedSet.zrange(key, start, stop, args);
    }

    @GET
    @Path("/sortedset/rangebyscore/{key}/{min}/{max}")
    public List<String> sortedSetRangeByScore(@PathParam("key") String key, @PathParam("min") double min,
            @PathParam("max") double max, @QueryParam("offset") long offset, @QueryParam("count") int count) {
        return sortedSet.zrangebyscore(key, ScoreRange.from(min, max), new ZRangeArgs().limit(offset, count));
    }

    @POST
    @Path("/sortedset/rangestore/{dst}/{src}/{min}/{max}")
    public long sortedSetRangeStore(@PathParam("dst") String dst, @PathParam("src") String src,
            @PathParam("min") long min, @PathParam("max") long max) {
        return sortedSet.zrangestore(dst, src, min, max);
    }

    @GET
    @Path("/sortedset/reactive/score/{key}/{member}")
    public Uni<Double> sortedSetScoreReactive(@PathParam("key") String key, @PathParam("member") String member) {
        return reactiveSortedSet.zscore(key, member);
    }

    @POST
    @Path("/bitmap/setbit/{key}/{offset}")
    public int bitmapSetBit(@PathParam("key") String key, @PathParam("offset") long offset, String value) {
        return bitmap.setbit(key, offset, Integer.parseInt(value));
    }

    @GET
    @Path("/bitmap/getbit/{key}/{offset}")
    public int bitmapGetBit(@PathParam("key") String key, @PathParam("offset") long offset) {
        return bitmap.getbit(key, offset);
    }

    @GET
    @Path("/bitmap/bitcount/{key}")
    public long bitmapBitCount(@PathParam("key") String key) {
        return bitmap.bitcount(key);
    }

    /**
     * Exercises the {@code #}-prefixed offset, {@code INCRBY} and {@code OVERFLOW} sub-commands: {@code #2}
     * with an 8-bit type is absolute bit 16, so the {@code GET} at bit 16 reads back the value just written.
     */
    @POST
    @Path("/bitmap/bitfield/{key}")
    public List<Long> bitmapBitField(@PathParam("key") String key, String value) {
        BitFieldArgs args = new BitFieldArgs()
                .overflow(BitFieldArgs.OverflowType.WRAP)
                .set(BitFieldArgs.signed(8), BitFieldArgs.typeWidthBasedOffset(2), Long.parseLong(value))
                .get(BitFieldArgs.signed(8), 16)
                .incrBy(BitFieldArgs.signed(8), BitFieldArgs.typeWidthBasedOffset(2), 1);
        return bitmap.bitfield(key, args);
    }

    @GET
    @Path("/bitmap/reactive/bitcount/{key}")
    public Uni<Long> bitmapBitCountReactive(@PathParam("key") String key) {
        return reactiveBitmap.bitcount(key);
    }

    @POST
    @Path("/hyperloglog/pfadd/{key}")
    public boolean hyperloglogPfAdd(@PathParam("key") String key, String value) {
        return hyperloglog.pfadd(key, value.split(","));
    }

    @GET
    @Path("/hyperloglog/pfcount/{key}")
    public long hyperloglogPfCount(@PathParam("key") String key) {
        return hyperloglog.pfcount(key);
    }

    @GET
    @Path("/hyperloglog/pfcount/{key1}/{key2}")
    public long hyperloglogPfCountUnion(@PathParam("key1") String key1, @PathParam("key2") String key2) {
        return hyperloglog.pfcount(key1, key2);
    }

    @POST
    @Path("/hyperloglog/pfmerge/{dest}/{src1}/{src2}")
    public void hyperloglogPfMerge(@PathParam("dest") String dest, @PathParam("src1") String src1,
            @PathParam("src2") String src2) {
        hyperloglog.pfmerge(dest, src1, src2);
    }

    @POST
    @Path("/hyperloglog/reactive/pfadd/{key}")
    public Uni<Boolean> hyperloglogPfAddReactive(@PathParam("key") String key, String value) {
        return reactiveHyperLogLog.pfadd(key, value.split(","));
    }

    @GET
    @Path("/hyperloglog/reactive/pfcount/{key}")
    public Uni<Long> hyperloglogPfCountReactive(@PathParam("key") String key) {
        return reactiveHyperLogLog.pfcount(key);
    }

    @POST
    @Path("/countmin/init/{key}/{width}/{depth}")
    public void countMinInitByDim(@PathParam("key") String key, @PathParam("width") long width,
            @PathParam("depth") long depth) {
        countmin.cmsInitByDim(key, width, depth);
    }

    @POST
    @Path("/countmin/initbyprob/{key}/{error}/{probability}")
    public void countMinInitByProb(@PathParam("key") String key, @PathParam("error") double error,
            @PathParam("probability") double probability) {
        countmin.cmsInitByProb(key, error, probability);
    }

    @POST
    @Path("/countmin/incrby/{key}/{item}/{increment}")
    public long countMinIncrBy(@PathParam("key") String key, @PathParam("item") String item,
            @PathParam("increment") long increment) {
        return countmin.cmsIncrBy(key, item, increment);
    }

    /**
     * Body: {@code item=increment,item=increment,...}. Uses an insertion-ordered map so the reply order is deterministic.
     */
    @POST
    @Path("/countmin/incrby/{key}")
    public Map<String, Long> countMinIncrByMany(@PathParam("key") String key, String couples) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String couple : couples.split(",")) {
            String[] parts = couple.split("=");
            map.put(parts[0], Long.parseLong(parts[1]));
        }
        return countmin.cmsIncrBy(key, map);
    }

    @GET
    @Path("/countmin/query/{key}/{item}")
    public long countMinQuery(@PathParam("key") String key, @PathParam("item") String item) {
        return countmin.cmsQuery(key, item);
    }

    @GET
    @Path("/countmin/query/{key}")
    public List<Long> countMinQueryMany(@PathParam("key") String key, @QueryParam("items") String items) {
        return countmin.cmsQuery(key, items.split(","));
    }

    /**
     * Merges {@code src1} and {@code src2} into {@code dest}. Weights are optional: {@code ?weights=2,1}.
     */
    @POST
    @Path("/countmin/merge/{dest}/{src1}/{src2}")
    public void countMinMerge(@PathParam("dest") String dest, @PathParam("src1") String src1,
            @PathParam("src2") String src2, @QueryParam("weights") String weights) {
        List<Integer> weightList = weights == null ? null
                : Arrays.stream(weights.split(",")).map(Integer::parseInt).toList();
        countmin.cmsMerge(dest, List.of(src1, src2), weightList);
    }

    @POST
    @Path("/countmin/reactive/incrby/{key}/{item}/{increment}")
    public Uni<Long> countMinIncrByReactive(@PathParam("key") String key, @PathParam("item") String item,
            @PathParam("increment") long increment) {
        return reactiveCountMin.cmsIncrBy(key, item, increment);
    }

    @GET
    @Path("/countmin/reactive/query/{key}/{item}")
    public Uni<Long> countMinQueryReactive(@PathParam("key") String key, @PathParam("item") String item) {
        return reactiveCountMin.cmsQuery(key, item);
    }

    @POST
    @Path("/autosuggest/add/{key}/{score}")
    public long autoSuggestAdd(@PathParam("key") String key, @PathParam("score") double score,
            @QueryParam("incr") boolean incr, String string) {
        return autosuggest.ftSugAdd(key, string, score, incr);
    }

    @DELETE
    @Path("/autosuggest/{key}/{string}")
    public boolean autoSuggestDel(@PathParam("key") String key, @PathParam("string") String string) {
        return autosuggest.ftSugDel(key, string);
    }

    @GET
    @Path("/autosuggest/len/{key}")
    public long autoSuggestLen(@PathParam("key") String key) {
        return autosuggest.ftSugLen(key);
    }

    @GET
    @Path("/autosuggest/get/{key}/{prefix}")
    public List<String> autoSuggestGet(@PathParam("key") String key, @PathParam("prefix") String prefix,
            @QueryParam("fuzzy") boolean fuzzy, @QueryParam("max") int max,
            @QueryParam("withScores") boolean withScores) {
        List<Suggestion> suggestions = fuzzy || max > 0 || withScores
                ? autosuggest.ftSugGet(key, prefix, getArgs(fuzzy, max, withScores))
                : autosuggest.ftSugGet(key, prefix);
        return formatSuggestions(suggestions, withScores);
    }

    @POST
    @Path("/autosuggest/reactive/add/{key}/{score}")
    public Uni<Long> autoSuggestAddReactive(@PathParam("key") String key, @PathParam("score") double score,
            @QueryParam("incr") boolean incr, String string) {
        return reactiveAutoSuggest.ftSugAdd(key, string, score, incr);
    }

    @GET
    @Path("/autosuggest/reactive/get/{key}/{prefix}")
    public Uni<List<String>> autoSuggestGetReactive(@PathParam("key") String key, @PathParam("prefix") String prefix,
            @QueryParam("fuzzy") boolean fuzzy, @QueryParam("max") int max,
            @QueryParam("withScores") boolean withScores) {
        return reactiveAutoSuggest.ftSugGet(key, prefix, getArgs(fuzzy, max, withScores))
                .map(suggestions -> formatSuggestions(suggestions, withScores));
    }

    private static GetArgs getArgs(boolean fuzzy, int max, boolean withScores) {
        GetArgs args = new GetArgs();
        if (fuzzy) {
            args.fuzzy();
        }
        if (max > 0) {
            args.max(max);
        }
        if (withScores) {
            args.withScores();
        }
        return args;
    }

    private static List<String> formatSuggestions(List<Suggestion> suggestions, boolean withScores) {
        return suggestions.stream()
                .map(s -> withScores ? s.suggestion() + ":" + s.score() : s.suggestion())
                .toList();
    }

    @POST
    @Path("/geo/add/{key}/{longitude}/{latitude}")
    public boolean geoAdd(@PathParam("key") String key, @PathParam("longitude") double longitude,
            @PathParam("latitude") double latitude, String member) {
        return geo.geoadd(key, longitude, latitude, member);
    }

    @GET
    @Path("/geo/dist/{key}/{from}/{to}")
    public Double geoDist(@PathParam("key") String key, @PathParam("from") String from, @PathParam("to") String to) {
        OptionalDouble distance = geo.geodist(key, from, to, GeoUnit.KM);
        return distance.isPresent() ? distance.getAsDouble() : null;
    }

    @GET
    @Path("/geo/hash/{key}/{member}")
    public String geoHash(@PathParam("key") String key, @PathParam("member") String member) {
        return geo.geohash(key, member).get(0);
    }

    @GET
    @Path("/geo/pos/{key}/{member}")
    public String geoPos(@PathParam("key") String key, @PathParam("member") String member) {
        GeoPosition position = geo.geopos(key, member).get(0);
        return position == null ? null : position.longitude() + "," + position.latitude();
    }

    @GET
    @Path("/geo/search/{key}")
    public List<String> geoSearch(@PathParam("key") String key, @QueryParam("longitude") double longitude,
            @QueryParam("latitude") double latitude, @QueryParam("radius") double radius) {
        GeoSearchArgs<String> args = new GeoSearchArgs<String>()
                .fromCoordinate(longitude, latitude)
                .byRadius(radius, GeoUnit.KM)
                .ascending()
                .withDistance();
        return geo.geosearch(key, args).stream()
                .map(v -> v.member() + "," + v.distance().getAsDouble())
                .toList();
    }

    @SuppressWarnings("deprecation")
    @GET
    @Path("/geo/radiusbymember/{key}/{member}/{radius}")
    public Set<String> geoRadiusByMember(@PathParam("key") String key, @PathParam("member") String member,
            @PathParam("radius") double radius) {
        return geo.georadiusbymember(key, member, radius, GeoUnit.KM);
    }

    @POST
    @Path("/geo/searchstore/{dest}/{key}/{member}/{radius}")
    public long geoSearchStore(@PathParam("dest") String dest, @PathParam("key") String key,
            @PathParam("member") String member, @PathParam("radius") double radius) {
        GeoSearchStoreArgs<String> args = new GeoSearchStoreArgs<String>()
                .fromMember(member)
                .byRadius(radius, GeoUnit.KM);
        return geo.geosearchstore(dest, key, args, false);
    }

    @POST
    @Path("/geo/reactive/add/{key}/{longitude}/{latitude}")
    public Uni<Boolean> geoAddReactive(@PathParam("key") String key, @PathParam("longitude") double longitude,
            @PathParam("latitude") double latitude, String member) {
        return reactiveGeo.geoadd(key, longitude, latitude, member);
    }

    @GET
    @Path("/geo/reactive/dist/{key}/{from}/{to}")
    public Uni<Double> geoDistReactive(@PathParam("key") String key, @PathParam("from") String from,
            @PathParam("to") String to) {
        return reactiveGeo.geodist(key, from, to, GeoUnit.KM);
    }

    @RegisterForReflection
    public static class JsonPerson {
        public String name;
        public int age;

        @Override
        public boolean equals(Object o) {
            return o instanceof JsonPerson other && age == other.age && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @POST
    @Path("/json/{key}")
    public String jsonSet(@PathParam("key") String key, @QueryParam("path") String path,
            @QueryParam("nx") boolean nx, @QueryParam("xx") boolean xx, String body) {
        String target = path == null ? "$" : path;
        Object value = Json.decodeValue(body);
        JsonSetArgs args = new JsonSetArgs();
        if (nx) {
            args.nx();
        }
        if (xx) {
            args.xx();
        }
        if (value instanceof JsonObject object) {
            json.jsonSet(key, target, object, args);
        } else if (value instanceof JsonArray array) {
            json.jsonSet(key, target, array, args);
        } else {
            json.jsonSet(key, target, value, args);
        }
        return encodeArray(json.jsonGet(key, "$"));
    }

    @GET
    @Path("/json/{key}")
    public String jsonGetObject(@PathParam("key") String key) {
        return encodeObject(json.jsonGetObject(key));
    }

    @GET
    @Path("/json/array/{key}")
    public String jsonGetArray(@PathParam("key") String key) {
        return encodeArray(json.jsonGetArray(key));
    }

    @GET
    @Path("/json/path/{key}")
    public String jsonGetPath(@PathParam("key") String key, @QueryParam("path") String path) {
        return encodeArray(json.jsonGet(key, path));
    }

    @GET
    @Path("/json/paths/{key}")
    public String jsonGetPaths(@PathParam("key") String key, @QueryParam("path") List<String> paths) {
        return encodeObject(json.jsonGet(key, paths.toArray(new String[0])));
    }

    @POST
    @Path("/json/person/{key}")
    public String jsonSetPerson(@PathParam("key") String key, String value) {
        String[] parts = value.split(",");
        JsonPerson person = new JsonPerson();
        person.name = parts[0];
        person.age = Integer.parseInt(parts[1]);
        json.jsonSet(key, person);
        JsonPerson read = json.jsonGet(key, JsonPerson.class);
        return read.name + "," + read.age + "," + person.equals(read);
    }

    @POST
    @Path("/json/arrappend/{key}")
    public List<Integer> jsonArrAppend(@PathParam("key") String key, @QueryParam("path") String path, String values) {
        return json.jsonArrAppend(key, path, values.split(","));
    }

    @GET
    @Path("/json/arrlen/{key}")
    public List<Integer> jsonArrLen(@PathParam("key") String key, @QueryParam("path") String path) {
        if (path == null) {
            OptionalInt length = json.jsonArrLen(key);
            return length.isPresent() ? List.of(length.getAsInt()) : List.of();
        }
        return json.jsonArrLen(key, path);
    }

    @POST
    @Path("/json/arrpop/{key}")
    public List<Integer> jsonArrPop(@PathParam("key") String key, @QueryParam("path") String path) {
        return json.jsonArrPop(key, Integer.class, path);
    }

    @GET
    @Path("/json/type/{key}")
    public List<String> jsonType(@PathParam("key") String key, @QueryParam("path") String path) {
        return json.jsonType(key, path);
    }

    @POST
    @Path("/json/numincrby/{key}")
    public String jsonNumIncrBy(@PathParam("key") String key, @QueryParam("path") String path, String value) {
        json.jsonNumincrby(key, path, Double.parseDouble(value));
        return encodeArray(json.jsonGet(key, path));
    }

    @POST
    @Path("/json/strappend/{key}")
    public List<Integer> jsonStrAppend(@PathParam("key") String key, @QueryParam("path") String path, String value) {
        return json.jsonStrAppend(key, path, value);
    }

    @GET
    @Path("/json/strlen/{key}")
    public List<Integer> jsonStrLen(@PathParam("key") String key, @QueryParam("path") String path) {
        return json.jsonStrLen(key, path);
    }

    @POST
    @Path("/json/toggle/{key}")
    public List<Boolean> jsonToggle(@PathParam("key") String key, @QueryParam("path") String path) {
        return json.jsonToggle(key, path);
    }

    @GET
    @Path("/json/objkeys/{key}")
    public List<String> jsonObjKeys(@PathParam("key") String key) {
        return json.jsonObjKeys(key);
    }

    @GET
    @Path("/json/objlen/{key}")
    public Integer jsonObjLen(@PathParam("key") String key) {
        OptionalInt length = json.jsonObjLen(key);
        return length.isPresent() ? length.getAsInt() : null;
    }

    @GET
    @Path("/json/mget")
    public List<String> jsonMget(@QueryParam("path") String path, @QueryParam("key") List<String> keys) {
        return json.jsonMget(path, keys.toArray(new String[0])).stream()
                .map(LettuceBackendResource::encodeArray)
                .toList();
    }

    @POST
    @Path("/json/clear/{key}")
    public int jsonClear(@PathParam("key") String key, @QueryParam("path") String path) {
        return path == null ? json.jsonClear(key) : json.jsonClear(key, path);
    }

    @DELETE
    @Path("/json/{key}")
    public int jsonDel(@PathParam("key") String key, @QueryParam("path") String path) {
        return path == null ? json.jsonDel(key) : json.jsonDel(key, path);
    }

    @POST
    @Path("/json/reactive/{key}")
    public Uni<String> jsonSetReactive(@PathParam("key") String key, String body) {
        return reactiveJson.jsonSet(key, "$", new JsonObject(body))
                .chain(() -> reactiveJson.jsonGetObject(key))
                .map(LettuceBackendResource::encodeObject);
    }

    @GET
    @Path("/json/reactive/{key}")
    public Uni<String> jsonGetReactive(@PathParam("key") String key, @QueryParam("path") String path) {
        return reactiveJson.jsonGet(key, path).map(LettuceBackendResource::encodeArray);
    }

    private static String encodeObject(JsonObject object) {
        return object == null ? null : object.encode();
    }

    private static String encodeArray(JsonArray array) {
        return array == null ? null : array.encode();
    }

    @GET
    @Path("/with-connection/client-ids")
    public String withConnectionClientIds() {
        long outside = blocking.execute("CLIENT", "ID").toLong();
        long[] inside = new long[2];
        blocking.withConnection(ds -> {
            inside[0] = ds.execute("CLIENT", "ID").toLong();
            inside[1] = ds.execute("CLIENT", "ID").toLong();
        });
        return inside[0] + "," + inside[1] + "," + outside;
    }

    @GET
    @Path("/with-connection/reactive/client-ids")
    public Uni<String> withConnectionClientIdsReactive() {
        long[] inside = new long[2];
        return reactive.execute("CLIENT", "ID").map(Response::toLong)
                .chain(outside -> reactive.withConnection(ds -> ds.execute("CLIENT", "ID").map(Response::toLong)
                        .invoke(id -> inside[0] = id)
                        .chain(() -> ds.execute("CLIENT", "ID").map(Response::toLong))
                        .invoke(id -> inside[1] = id)
                        .replaceWithVoid())
                        .map(ignored -> inside[0] + "," + inside[1] + "," + outside));
    }

    @GET
    @Path("/with-connection/nested")
    public String withConnectionNested() {
        long[] ids = new long[2];
        blocking.withConnection(outer -> {
            ids[0] = outer.execute("CLIENT", "ID").toLong();
            outer.withConnection(inner -> ids[1] = inner.execute("CLIENT", "ID").toLong());
        });
        return ids[0] + "," + ids[1];
    }

    @POST
    @Path("/with-transaction/blocking/{key}")
    public String withTransactionBlocking(@PathParam("key") String key, String value) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var v = tx.value(String.class, String.class);
            v.set(key, value);
            v.get(key);
        });
        return result.discarded() + "," + result.size() + "," + result.get(1);
    }

    @POST
    @Path("/with-transaction/reactive/{key}")
    public Uni<String> withTransactionReactive(@PathParam("key") String key, String value) {
        return reactive.withTransaction(tx -> {
            var v = tx.value(String.class, String.class);
            return v.set(key, value).chain(() -> v.get(key));
        }).map(result -> result.discarded() + "," + result.size() + "," + result.get(1));
    }

    @POST
    @Path("/with-transaction/discard/{key}")
    public String withTransactionDiscard(@PathParam("key") String key, String value) {
        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class, String.class).set(key, value);
            tx.discard();
        });
        return result.discarded() + "," + values.get(key);
    }

    @POST
    @Path("/with-transaction/optimistic/{key}")
    public String withTransactionOptimistic(@PathParam("key") String key, String suffix) {
        OptimisticLockingTransactionResult<String> result = blocking.withTransaction(
                preTx -> preTx.value(String.class, String.class).get(key),
                (current, tx) -> tx.value(String.class, String.class).set(key, current + suffix),
                key);
        return result.discarded() + "," + result.getPreTransactionResult() + "," + values.get(key);
    }

    @POST
    @Path("/with-transaction/key/{key}")
    public String withTransactionKey(@PathParam("key") String key) {
        values.set(key, "v");
        TransactionResult result = blocking.withTransaction(tx -> {
            var k = tx.key(String.class);
            k.exists(key);
            k.expire(key, 100);
            k.ttl(key);
            k.type(key);
        });
        boolean exists = result.get(0);
        boolean expired = result.get(1);
        long ttl = result.get(2);
        RedisValueType type = result.get(3);
        return result.discarded() + "," + result.size() + "," + exists + "," + expired + "," + (ttl > 0) + "," + type;
    }

    @POST
    @Path("/with-transaction/sortedset/{key}")
    public String withTransactionSortedSet(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var s = tx.sortedSet(String.class);
            s.zadd(key, 1.0, "a");
            s.zadd(key, Map.of("b", 2.0, "c", 3.0));
            s.zcard(key);
            s.zpopmin(key);
            s.zrangeWithScores(key, 0, -1, new ZRangeArgs().rev());
            s.zrangestore(key + "-dst", key, 0, -1);
        });
        boolean added = result.get(0);
        int addedCount = result.get(1);
        long card = result.get(2);
        ScoredValue<String> min = result.get(3);
        List<ScoredValue<String>> reversed = result.get(4);
        long stored = result.get(5);
        StringBuilder sb = new StringBuilder();
        sb.append(result.discarded()).append(',').append(result.size()).append(',')
                .append(added).append(',').append(addedCount).append(',').append(card).append(',')
                .append(min.value()).append(',').append(min.score());
        for (ScoredValue<String> value : reversed) {
            sb.append(',').append(value.value()).append(':').append(value.score());
        }
        return sb.append(',').append(stored).toString();
    }

    @POST
    @Path("/with-transaction/hyperloglog/{key}")
    public String withTransactionHyperLogLog(@PathParam("key") String key) {
        String other = key + "-other";
        String merged = key + "-merged";
        TransactionResult result = blocking.withTransaction(tx -> {
            var h = tx.hyperloglog(String.class, String.class);
            h.pfadd(key, "a", "b", "c");
            h.pfadd(key, "a");
            h.pfadd(other, "c", "d");
            h.pfmerge(merged, key, other);
            h.pfcount(merged);
        });
        boolean added = result.get(0);
        boolean addedAgain = result.get(1);
        boolean addedOther = result.get(2);
        Object mergeResult = result.get(3);
        long count = result.get(4);
        return result.discarded() + "," + result.size() + "," + added + "," + addedAgain + "," + addedOther + ","
                + (mergeResult == null) + "," + count;
    }

    @POST
    @Path("/with-transaction/geo/{key}")
    public String withTransactionGeo(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var g = tx.geo(String.class, String.class);
            g.geoadd(key, 13.361389, 38.115556, "Palermo");
            g.geoadd(key, 15.087269, 37.502669, "Catania");
            g.geodist(key, "Palermo", "Catania", GeoUnit.KM);
            g.geopos(key, "Palermo", "missing");
            g.geosearch(key, new GeoSearchArgs<String>().fromMember("Palermo").byRadius(200, GeoUnit.KM).ascending());
        });
        boolean addedPalermo = result.get(0);
        boolean addedCatania = result.get(1);
        double distance = result.get(2);
        List<GeoPosition> positions = result.get(3);
        List<GeoValue<String>> found = result.get(4);
        StringBuilder sb = new StringBuilder();
        sb.append(result.discarded()).append(',').append(result.size()).append(',')
                .append(addedPalermo).append(',').append(addedCatania).append(',')
                .append(Math.round(distance)).append(',')
                .append(positions.size()).append(',').append(positions.get(1) == null);
        for (GeoValue<String> value : found) {
            sb.append(',').append(value.member());
        }
        return sb.toString();
    }

    @POST
    @Path("/with-transaction/json/{key}")
    public String withTransactionJson(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var j = tx.json();
            j.jsonSet(key, JsonObject.of("name", "luke", "tags", JsonArray.of("jedi"), "age", 20));
            j.jsonArrAppend(key, "$.tags", "pilot");
            j.jsonArrLen(key, "$.tags");
            j.jsonNumincrby(key, "$.age", 2);
            j.jsonStrAppend(key, "$.name", " skywalker");
            j.jsonType(key, "$.age");
            j.jsonGet(key);
            j.jsonDel(key, "$.tags");
            j.jsonObjKeys(key, "$");
        });
        Object set = result.get(0);
        List<Integer> appended = result.get(1);
        List<Integer> length = result.get(2);
        Object incremented = result.get(3);
        List<Integer> strAppended = result.get(4);
        List<String> type = result.get(5);
        JsonObject document = result.get(6);
        int deleted = result.get(7);
        List<List<String>> keys = result.get(8);
        return result.discarded() + "," + result.size() + "," + (set == null) + "," + appended + "," + length + ","
                + (incremented == null) + "," + strAppended + "," + type + "," + document.encode() + "," + deleted + ","
                + keys;
    }

    @POST
    @Path("/with-transaction/countmin/{key}")
    public String withTransactionCountMin(@PathParam("key") String key) {
        String other = key + "-other";
        String merged = key + "-merged";
        TransactionResult result = blocking.withTransaction(tx -> {
            var c = tx.countmin(String.class, String.class);
            c.cmsInitByDim(key, 100, 5);
            c.cmsInitByDim(other, 100, 5);
            c.cmsInitByDim(merged, 100, 5);
            c.cmsIncrBy(key, "a", 3);
            Map<String, Long> couples = new LinkedHashMap<>();
            couples.put("a", 2L);
            couples.put("b", 4L);
            c.cmsIncrBy(other, couples);
            c.cmsMerge(merged, List.of(key, other), List.of(2, 1));
            c.cmsQuery(merged, "a", "b");
        });
        Object init = result.get(0);
        long incremented = result.get(3);
        List<Long> counts = result.get(4);
        Object mergeResult = result.get(5);
        List<Long> queried = result.get(6);
        return result.discarded() + "," + result.size() + "," + (init == null) + "," + incremented + "," + counts + ","
                + (mergeResult == null) + "," + queried;
    }

    @POST
    @Path("/with-transaction/autosuggest/{key}")
    public String withTransactionAutoSuggest(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var a = tx.autosuggest(String.class);
            a.ftSugAdd(key, "abc", 1.0);
            a.ftSugAdd(key, "abcd", 1.0);
            a.ftSugAdd(key, "abcde", 2.0);
            a.ftSugAdd(key, "boo", 20);
            a.ftSugDel(key, "boo");
            a.ftSugLen(key);
            a.ftSugget(key, "abcd");
            a.ftSugget(key, "ab", new GetArgs().max(1).withScores());
        });
        long first = result.get(0);
        long last = result.get(3);
        boolean deleted = result.get(4);
        long len = result.get(5);
        List<Suggestion> plain = result.get(6);
        List<Suggestion> scored = result.get(7);
        return result.discarded() + "," + result.size() + "," + first + "," + last + "," + deleted + "," + len + ","
                + plain.stream().map(Suggestion::suggestion).sorted().toList() + "," + formatSuggestions(scored, true);
    }

    @RegisterForReflection
    public record SearchDocument(String key, double score, Map<String, String> properties) {
    }

    @RegisterForReflection
    public record SearchResult(int count, long cursor, List<SearchDocument> documents) {
    }

    @RegisterForReflection
    public record SpellCheck(boolean correct, Map<String, List<String>> suggestions) {
    }

    @POST
    @Path("/search/index/{index}")
    public void searchCreate(@PathParam("index") String index, @QueryParam("prefix") String prefix,
            @QueryParam("json") boolean onJson, String fields) {
        CreateArgs args = onJson ? new CreateArgs().onJson() : new CreateArgs().onHash();
        if (prefix != null) {
            args.prefixes(prefix);
        }
        for (IndexedField field : parseFields(fields)) {
            if (field.getAlias() == null) {
                args.indexedField(field.getField(), field.getType(), field.getOptions());
            } else {
                args.indexedField(field.getField(), field.getAlias(), field.getType(), field.getOptions());
            }
        }
        search.ftCreate(index, args);
    }

    private static List<IndexedField> parseFields(String spec) {
        List<IndexedField> fields = new ArrayList<>();
        for (String token : spec.split(",")) {
            String[] parts = token.trim().split(":");
            String[] name = parts[0].split("=");
            FieldType type = FieldType.valueOf(parts[1].toUpperCase());
            FieldOptions options = new FieldOptions();
            if (parts.length > 2 && parts[2].equalsIgnoreCase("sortable")) {
                options.sortable();
            }
            fields.add(name.length > 1 ? IndexedField.from(name[0], name[1], type, options)
                    : IndexedField.from(name[0], type, options));
        }
        return fields;
    }

    @GET
    @Path("/search/list")
    public List<String> searchList() {
        return search.ft_list();
    }

    @DELETE
    @Path("/search/index/{index}")
    public void searchDrop(@PathParam("index") String index, @QueryParam("dd") boolean dd) {
        search.ftDropIndex(index, dd);
    }

    @POST
    @Path("/search/alter/{index}")
    public void searchAlter(@PathParam("index") String index, String field) {
        search.ftAlter(index, parseFields(field).get(0));
    }

    @GET
    @Path("/search/query/{index}")
    public SearchResult searchQuery(@PathParam("index") String index, @QueryParam("q") String query,
            @QueryParam("return") String returnFields, @QueryParam("sortBy") String sortBy,
            @QueryParam("desc") boolean desc, @QueryParam("offset") Integer offset, @QueryParam("count") Integer count,
            @QueryParam("withScores") boolean withScores, @QueryParam("noContent") boolean noContent,
            @QueryParam("param") List<String> params) {
        QueryArgs args = new QueryArgs();
        boolean withArgs = false;
        if (returnFields != null) {
            for (String field : returnFields.split(",")) {
                String[] parts = field.split("=");
                if (parts.length > 1) {
                    args.returnAttribute(parts[0], parts[1]);
                } else {
                    args.returnAttribute(parts[0]);
                }
            }
            withArgs = true;
        }
        if (sortBy != null) {
            if (desc) {
                args.sortByDescending(sortBy);
            } else {
                args.sortByAscending(sortBy);
            }
            withArgs = true;
        }
        if (offset != null && count != null) {
            args.limit(offset, count);
            withArgs = true;
        }
        if (withScores) {
            args.withScores();
            withArgs = true;
        }
        if (noContent) {
            args.nocontent();
            withArgs = true;
        }
        if (!params.isEmpty()) {
            for (String param : params) {
                String[] parts = param.split("=", 2);
                args.param(parts[0], parts[1]);
            }
            args.dialect(2);
            withArgs = true;
        }
        return toResult(withArgs ? search.ftSearch(index, query, args) : search.ftSearch(index, query));
    }

    @GET
    @Path("/search/aggregate/{index}")
    public SearchResult searchAggregate(@PathParam("index") String index, @QueryParam("q") String query,
            @QueryParam("load") String load, @QueryParam("apply") String apply, @QueryParam("groupBy") String groupBy,
            @QueryParam("reduce") List<String> reduces, @QueryParam("sortBy") String sortBy,
            @QueryParam("desc") boolean desc, @QueryParam("cursor") Integer cursorCount) {
        AggregateArgs args = new AggregateArgs();
        if ("*".equals(load)) {
            args.allFields();
        } else if (load != null) {
            for (String field : load.split(",")) {
                String[] parts = field.split("=");
                if (parts.length > 1) {
                    args.field(parts[0], parts[1]);
                } else {
                    args.field(parts[0]);
                }
            }
        }
        if (apply != null) {
            String[] parts = apply.split("=", 2);
            args.apply(new AggregateArgs.Apply(parts[1], parts[0]));
        }
        if (groupBy != null) {
            AggregateArgs.GroupBy group = new AggregateArgs.GroupBy();
            for (String property : groupBy.split(",")) {
                group.addProperty(property);
            }
            for (String reduce : reduces) {
                String[] parts = reduce.split(":");
                Object[] reduceArgs = parts.length > 2 ? parts[2].split(",") : new Object[0];
                group.addReduceFunction(parts[0], parts[1], reduceArgs);
            }
            args.groupBy(group);
        }
        if (sortBy != null) {
            args.sortBy(desc ? new AggregateArgs.SortBy().descending(sortBy) : new AggregateArgs.SortBy().ascending(sortBy));
        }
        if (cursorCount != null) {
            args.withCursor().cursorCount(cursorCount);
        }
        return toResult(search.ftAggregate(index, query, args));
    }

    @GET
    @Path("/search/cursor/{index}/{cursor}")
    public SearchResult searchCursorRead(@PathParam("index") String index, @PathParam("cursor") long cursor,
            @QueryParam("count") Integer count) {
        return toResult(count == null ? search.ftCursorRead(index, cursor) : search.ftCursorRead(index, cursor, count));
    }

    @DELETE
    @Path("/search/cursor/{index}/{cursor}")
    public void searchCursorDel(@PathParam("index") String index, @PathParam("cursor") long cursor) {
        search.ftCursorDel(index, cursor);
    }

    @POST
    @Path("/search/alias/{alias}/{index}")
    public void searchAliasAdd(@PathParam("alias") String alias, @PathParam("index") String index,
            @QueryParam("update") boolean update) {
        if (update) {
            search.ftAliasUpdate(alias, index);
        } else {
            search.ftAliasAdd(alias, index);
        }
    }

    @DELETE
    @Path("/search/alias/{alias}")
    public void searchAliasDel(@PathParam("alias") String alias) {
        search.ftAliasDel(alias);
    }

    @POST
    @Path("/search/synonyms/{index}/{group}")
    public void searchSynUpdate(@PathParam("index") String index, @PathParam("group") String group,
            @QueryParam("skipInitialScan") boolean skipInitialScan, String terms) {
        search.ftSynUpdate(index, group, skipInitialScan, terms.split(","));
    }

    @GET
    @Path("/search/synonyms/{index}")
    public Map<String, List<String>> searchSynDump(@PathParam("index") String index) {
        SynDumpResponse response = search.ftSynDump(index);
        Map<String, List<String>> groups = new TreeMap<>();
        for (String group : response.groups()) {
            groups.put(group, response.synonym(group).stream().sorted().toList());
        }
        return groups;
    }

    @POST
    @Path("/search/dict/{dict}")
    public void searchDictAdd(@PathParam("dict") String dict, String words) {
        search.ftDictAdd(dict, words.split(","));
    }

    @DELETE
    @Path("/search/dict/{dict}")
    public void searchDictDel(@PathParam("dict") String dict, @QueryParam("words") String words) {
        search.ftDictDel(dict, words.split(","));
    }

    @GET
    @Path("/search/dict/{dict}")
    public List<String> searchDictDump(@PathParam("dict") String dict) {
        return search.ftDictDump(dict).stream().sorted().toList();
    }

    @GET
    @Path("/search/spellcheck/{index}")
    public SpellCheck searchSpellCheck(@PathParam("index") String index, @QueryParam("q") String query,
            @QueryParam("include") String include, @QueryParam("exclude") String exclude,
            @QueryParam("distance") Integer distance) {
        SpellCheckResponse response;
        if (include == null && exclude == null && distance == null) {
            response = search.ftSpellCheck(index, query);
        } else {
            SpellCheckArgs args = new SpellCheckArgs();
            if (include != null) {
                args.includes(include);
            }
            if (exclude != null) {
                args.excludes(exclude);
            }
            if (distance != null) {
                args.distance(distance);
            }
            response = search.ftSpellCheck(index, query, args);
        }
        Map<String, List<String>> suggestions = new TreeMap<>();
        for (String word : response.misspelledWords()) {
            suggestions.put(word, response.suggestions(word).stream()
                    .map(SpellCheckResponse.SpellCheckSuggestion::word)
                    .toList());
        }
        return new SpellCheck(response.isCorrect(), suggestions);
    }

    @GET
    @Path("/search/tagvals/{index}/{field}")
    public Set<String> searchTagVals(@PathParam("index") String index, @PathParam("field") String field) {
        return new TreeSet<>(search.ftTagVals(index, field));
    }

    @GET
    @Path("/search/reactive/list")
    public Uni<List<String>> searchListReactive() {
        return reactiveSearch.ft_list();
    }

    @GET
    @Path("/search/reactive/query/{index}")
    public Uni<SearchResult> searchQueryReactive(@PathParam("index") String index, @QueryParam("q") String query) {
        return reactiveSearch.ftSearch(index, query).map(LettuceBackendResource::toResult);
    }

    private static SearchResult toResult(SearchQueryResponse response) {
        List<SearchDocument> documents = response.documents().stream()
                .map(d -> new SearchDocument(d.key(), d.score(), toStrings(d.properties())))
                .toList();
        return new SearchResult(response.count(), -1, documents);
    }

    private static SearchResult toResult(AggregationResponse response) {
        List<SearchDocument> documents = response.documents().stream()
                .map(d -> new SearchDocument(null, 0, toStrings(d.properties())))
                .toList();
        return new SearchResult(response.count(), response.cursor(), documents);
    }

    private static Map<String, String> toStrings(Map<String, Document.Property> properties) {
        Map<String, String> strings = new LinkedHashMap<>();
        properties.forEach((name, property) -> strings.put(name, property == null ? null : property.asString()));
        return strings;
    }

    @POST
    @Path("/with-transaction/search/{index}")
    public String withTransactionSearch(@PathParam("index") String index) {
        String alias = index + "-alias";
        String dict = index + "-dict";
        TransactionResult result = blocking.withTransaction(tx -> {
            var s = tx.search();
            s.ft_list(); // 0
            s.ftTagVals(index, "genre"); // 1
            s.ftAliasAdd(alias, index); // 2
            s.ftAliasDel(alias); // 3
            s.ftSynUpdate(index, "greetings", "hello", "world"); // 4
            s.ftSynDump(index); // 5
            s.ftDictAdd(dict, "hello", "bonjour"); // 6
            s.ftDictDel(dict, "hello"); // 7
            s.ftDictDump(dict); // 8
            s.ftAlter(index, IndexedField.from("plot", FieldType.TEXT)); // 9
            s.ftDropIndex(index, true); // 10
        });
        List<String> indexes = result.get(0);
        Set<String> genres = result.get(1);
        Object aliasAdded = result.get(2);
        SynDumpResponse synonyms = result.get(5);
        List<String> dictionary = result.get(8);
        Object dropped = result.get(10);
        return result.discarded() + "," + result.size() + "," + indexes.contains(index) + "," + new TreeSet<>(genres) + ","
                + (aliasAdded == null) + "," + synonyms.groups() + ","
                + synonyms.synonym("greetings").stream().sorted().toList() + "," + dictionary + "," + (dropped == null);
    }

}
