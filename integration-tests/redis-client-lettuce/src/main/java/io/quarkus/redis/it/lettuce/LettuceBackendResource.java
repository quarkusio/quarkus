package io.quarkus.redis.it.lettuce;

import java.time.Duration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
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
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
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
import io.quarkus.redis.datasource.stream.ClaimedMessages;
import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
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
    private final RedisDataSource provided;
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
    private final StreamCommands<String, String, String> stream;
    private final ReactiveStreamCommands<String, String, String> reactiveStream;
    private final PubSubCommands<String> pubsub;
    private final ReactivePubSubCommands<String> reactivePubSub;
    private final JsonCommands<String> json;
    private final ReactiveJsonCommands<String> reactiveJson;
    private final SearchCommands<String> search;
    private final ReactiveSearchCommands<String> reactiveSearch;
    private final AutoSuggestCommands<String> autosuggest;
    private final ReactiveAutoSuggestCommands<String> reactiveAutoSuggest;
    private final CuckooCommands<String, String> cuckoo;
    private final ReactiveCuckooCommands<String, String> reactiveCuckoo;
    private final BloomCommands<String, String> bloom;
    private final ReactiveBloomCommands<String, String> reactiveBloom;
    private final CountMinCommands<String, String> countmin;
    private final ReactiveCountMinCommands<String, String> reactiveCountMin;
    private final TopKCommands<String, String> topk;
    private final ReactiveTopKCommands<String, String> reactiveTopK;

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger subscriptionIds = new AtomicInteger();

    @Inject
    public LettuceBackendResource(RedisDataSource ds, ReactiveRedisDataSource reactiveDs,
            @RedisClientName("secure") RedisDataSource secureDs,
            @RedisClientName("cluster") RedisDataSource clusterDs,
            @RedisClientName("cluster") RedisClusterClient clusterClient,
            @RedisClientName("sentinel") RedisDataSource sentinelDs,
            @RedisClientName("replication") RedisDataSource replicationDs,
            @RedisClientName("provided") RedisDataSource providedDs) {
        this.blocking = ds;
        this.reactive = reactiveDs;
        this.secure = secureDs;
        this.cluster = clusterDs;
        this.clusterClient = clusterClient;
        this.sentinel = sentinelDs;
        this.replication = replicationDs;
        this.provided = providedDs;
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
        this.stream = ds.stream(String.class);
        this.reactiveStream = reactiveDs.stream(String.class);
        this.pubsub = ds.pubsub(String.class);
        this.reactivePubSub = reactiveDs.pubsub(String.class);
        this.json = ds.json();
        this.reactiveJson = reactiveDs.json();
        this.search = ds.search();
        this.reactiveSearch = reactiveDs.search();
        this.autosuggest = ds.autosuggest(String.class);
        this.reactiveAutoSuggest = reactiveDs.autosuggest(String.class);
        this.cuckoo = ds.cuckoo(String.class);
        this.reactiveCuckoo = reactiveDs.cuckoo(String.class);
        this.bloom = ds.bloom(String.class);
        this.reactiveBloom = reactiveDs.bloom(String.class);
        this.countmin = ds.countmin(String.class);
        this.reactiveCountMin = reactiveDs.countmin(String.class);
        this.topk = ds.topk(String.class);
        this.reactiveTopK = reactiveDs.topk(String.class);
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

    /**
     * Pings the {@code provided} client, whose hosts come from the {@link LettuceHostsProvider} bean named by
     * {@code hosts-provider-name}.
     */
    @GET
    @Path("/provided/ping")
    public String providedPing() {
        return provided.execute("PING").toString();
    }

    /**
     * The database the {@code provided} client is on, from {@code CLIENT INFO}: {@code 3}, the one of the provided URI.
     */
    @GET
    @Path("/provided/database")
    public String providedDatabase() {
        String info = provided.execute("CLIENT", "INFO").toString();
        for (String field : info.trim().split(" ")) {
            if (field.startsWith("db=")) {
                return field.substring("db=".length());
            }
        }
        return "unknown";
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
    @Path("/cuckoo/cfadd/{key}")
    public void cuckooCfAdd(@PathParam("key") String key, String value) {
        cuckoo.cfadd(key, value);
    }

    @POST
    @Path("/cuckoo/cfaddnx/{key}")
    public boolean cuckooCfAddNx(@PathParam("key") String key, String value) {
        return cuckoo.cfaddnx(key, value);
    }

    @GET
    @Path("/cuckoo/cfexists/{key}/{value}")
    public boolean cuckooCfExists(@PathParam("key") String key, @PathParam("value") String value) {
        return cuckoo.cfexists(key, value);
    }

    @GET
    @Path("/cuckoo/cfcount/{key}/{value}")
    public long cuckooCfCount(@PathParam("key") String key, @PathParam("value") String value) {
        return cuckoo.cfcount(key, value);
    }

    @DELETE
    @Path("/cuckoo/cfdel/{key}/{value}")
    public boolean cuckooCfDel(@PathParam("key") String key, @PathParam("value") String value) {
        return cuckoo.cfdel(key, value);
    }

    @GET
    @Path("/cuckoo/cfmexists/{key}")
    public List<Boolean> cuckooCfMExists(@PathParam("key") String key, @QueryParam("values") String values) {
        return cuckoo.cfmexists(key, values.split(","));
    }

    @POST
    @Path("/cuckoo/cfreserve/{key}/{capacity}")
    public void cuckooCfReserve(@PathParam("key") String key, @PathParam("capacity") long capacity,
            @QueryParam("bucketSize") Long bucketSize, @QueryParam("maxIterations") Integer maxIterations,
            @QueryParam("expansion") Integer expansion) {
        CfReserveArgs args = new CfReserveArgs();
        if (bucketSize != null) {
            args.bucketSize(bucketSize);
        }
        if (maxIterations != null) {
            args.maxIterations(maxIterations);
        }
        if (expansion != null) {
            args.expansion(expansion);
        }
        cuckoo.cfreserve(key, capacity, args);
    }

    @POST
    @Path("/cuckoo/cfinsert/{key}")
    public void cuckooCfInsert(@PathParam("key") String key, @QueryParam("capacity") Long capacity,
            @QueryParam("nocreate") boolean nocreate, String values) {
        cuckoo.cfinsert(key, cfInsertArgs(capacity, nocreate), values.split(","));
    }

    @POST
    @Path("/cuckoo/cfinsertnx/{key}")
    public List<Boolean> cuckooCfInsertNx(@PathParam("key") String key, @QueryParam("capacity") Long capacity,
            @QueryParam("nocreate") boolean nocreate, String values) {
        return cuckoo.cfinsertnx(key, cfInsertArgs(capacity, nocreate), values.split(","));
    }

    private static CfInsertArgs cfInsertArgs(Long capacity, boolean nocreate) {
        CfInsertArgs args = new CfInsertArgs();
        if (capacity != null) {
            args.capacity(capacity);
        }
        if (nocreate) {
            args.nocreate();
        }
        return args;
    }

    @POST
    @Path("/cuckoo/reactive/cfadd/{key}")
    public Uni<Void> cuckooCfAddReactive(@PathParam("key") String key, String value) {
        return reactiveCuckoo.cfadd(key, value);
    }

    @POST
    @Path("/cuckoo/reactive/cfaddnx/{key}")
    public Uni<Boolean> cuckooCfAddNxReactive(@PathParam("key") String key, String value) {
        return reactiveCuckoo.cfaddnx(key, value);
    }

    @GET
    @Path("/cuckoo/reactive/cfexists/{key}/{value}")
    public Uni<Boolean> cuckooCfExistsReactive(@PathParam("key") String key, @PathParam("value") String value) {
        return reactiveCuckoo.cfexists(key, value);
    }

    @GET
    @Path("/cuckoo/reactive/cfcount/{key}/{value}")
    public Uni<Long> cuckooCfCountReactive(@PathParam("key") String key, @PathParam("value") String value) {
        return reactiveCuckoo.cfcount(key, value);
    }

    @GET
    @Path("/cuckoo/reactive/cfmexists/{key}")
    public Uni<List<Boolean>> cuckooCfMExistsReactive(@PathParam("key") String key,
            @QueryParam("values") String values) {
        return reactiveCuckoo.cfmexists(key, values.split(","));
    }

    @POST
    @Path("/bloom/bfadd/{key}")
    public boolean bloomBfAdd(@PathParam("key") String key, String value) {
        return bloom.bfadd(key, value);
    }

    @GET
    @Path("/bloom/bfexists/{key}/{value}")
    public boolean bloomBfExists(@PathParam("key") String key, @PathParam("value") String value) {
        return bloom.bfexists(key, value);
    }

    @POST
    @Path("/bloom/bfmadd/{key}")
    public List<Boolean> bloomBfMAdd(@PathParam("key") String key, String values) {
        return bloom.bfmadd(key, values.split(","));
    }

    @GET
    @Path("/bloom/bfmexists/{key}")
    public List<Boolean> bloomBfMExists(@PathParam("key") String key, @QueryParam("values") String values) {
        return bloom.bfmexists(key, values.split(","));
    }

    @POST
    @Path("/bloom/bfreserve/{key}/{errorRate}/{capacity}")
    public void bloomBfReserve(@PathParam("key") String key, @PathParam("errorRate") double errorRate,
            @PathParam("capacity") long capacity, @QueryParam("nonScaling") boolean nonScaling) {
        BfReserveArgs args = new BfReserveArgs();
        if (nonScaling) {
            args.nonScaling();
        }
        bloom.bfreserve(key, errorRate, capacity, args);
    }

    @POST
    @Path("/bloom/bfinsert/{key}")
    public List<Boolean> bloomBfInsert(@PathParam("key") String key, @QueryParam("capacity") Long capacity,
            @QueryParam("nocreate") boolean nocreate, String values) {
        BfInsertArgs args = new BfInsertArgs();
        if (capacity != null) {
            args.capacity(capacity);
        }
        if (nocreate) {
            args.nocreate();
        }
        return bloom.bfinsert(key, args, values.split(","));
    }

    @POST
    @Path("/bloom/reactive/bfadd/{key}")
    public Uni<Boolean> bloomBfAddReactive(@PathParam("key") String key, String value) {
        return reactiveBloom.bfadd(key, value);
    }

    @GET
    @Path("/bloom/reactive/bfexists/{key}/{value}")
    public Uni<Boolean> bloomBfExistsReactive(@PathParam("key") String key, @PathParam("value") String value) {
        return reactiveBloom.bfexists(key, value);
    }

    @POST
    @Path("/bloom/reactive/bfmadd/{key}")
    public Uni<List<Boolean>> bloomBfMAddReactive(@PathParam("key") String key, String values) {
        return reactiveBloom.bfmadd(key, values.split(","));
    }

    @GET
    @Path("/bloom/reactive/bfmexists/{key}")
    public Uni<List<Boolean>> bloomBfMExistsReactive(@PathParam("key") String key,
            @QueryParam("values") String values) {
        return reactiveBloom.bfmexists(key, values.split(","));
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
    @Path("/topk/reserve/{key}/{topk}")
    public void topKReserve(@PathParam("key") String key, @PathParam("topk") int topk) {
        this.topk.topkReserve(key, topk);
    }

    @POST
    @Path("/topk/reserve/{key}/{topk}/{width}/{depth}/{decay}")
    public void topKReserve(@PathParam("key") String key, @PathParam("topk") int topk, @PathParam("width") int width,
            @PathParam("depth") int depth, @PathParam("decay") double decay) {
        this.topk.topkReserve(key, topk, width, depth, decay);
    }

    /**
     * Returns the item evicted by this addition, or the literal {@code "null"} when nothing was evicted.
     */
    @POST
    @Path("/topk/add/{key}/{item}")
    public String topKAdd(@PathParam("key") String key, @PathParam("item") String item) {
        return String.valueOf(topk.topkAdd(key, item).orElse(null));
    }

    /**
     * Body: {@code item,item,...}. Replies with one entry per item: the evicted item or {@code null}.
     */
    @POST
    @Path("/topk/add/{key}")
    public List<String> topKAddMany(@PathParam("key") String key, String items) {
        return topk.topkAdd(key, items.split(","));
    }

    @POST
    @Path("/topk/incrby/{key}/{item}/{increment}")
    public String topKIncrBy(@PathParam("key") String key, @PathParam("item") String item,
            @PathParam("increment") int increment) {
        return String.valueOf(topk.topkIncrBy(key, item, increment).orElse(null));
    }

    /**
     * Body: {@code item=increment,item=increment,...}. Uses an insertion-ordered map so the reply order is deterministic.
     * The reply maps each item to the item it evicted, or to {@code null}.
     */
    @POST
    @Path("/topk/incrby/{key}")
    public Map<String, String> topKIncrByMany(@PathParam("key") String key, String couples) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String couple : couples.split(",")) {
            String[] parts = couple.split("=");
            map.put(parts[0], Integer.parseInt(parts[1]));
        }
        return topk.topkIncrBy(key, map);
    }

    @GET
    @Path("/topk/list/{key}")
    public List<String> topKList(@PathParam("key") String key) {
        return topk.topkList(key);
    }

    @GET
    @Path("/topk/list/{key}/withcount")
    public Map<String, Integer> topKListWithCount(@PathParam("key") String key) {
        return topk.topkListWithCount(key);
    }

    @GET
    @Path("/topk/query/{key}/{item}")
    public boolean topKQuery(@PathParam("key") String key, @PathParam("item") String item) {
        return topk.topkQuery(key, item);
    }

    @GET
    @Path("/topk/query/{key}")
    public List<Boolean> topKQueryMany(@PathParam("key") String key, @QueryParam("items") String items) {
        return topk.topkQuery(key, items.split(","));
    }

    @POST
    @Path("/topk/reactive/add/{key}/{item}")
    public Uni<String> topKAddReactive(@PathParam("key") String key, @PathParam("item") String item) {
        return reactiveTopK.topkAdd(key, item).map(String::valueOf);
    }

    @GET
    @Path("/topk/reactive/list/{key}/withcount")
    public Uni<Map<String, Integer>> topKListWithCountReactive(@PathParam("key") String key) {
        return reactiveTopK.topkListWithCount(key);
    }

    @GET
    @Path("/topk/reactive/query/{key}/{item}")
    public Uni<Boolean> topKQueryReactive(@PathParam("key") String key, @PathParam("item") String item) {
        return reactiveTopK.topkQuery(key, item);
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
    @Path("/pubsub/subscribe")
    public String pubSubSubscribe(@QueryParam("channels") String channels) {
        Subscription subscription = new Subscription();
        subscription.subscriber = pubsub.subscribe(List.of(channels.split(",")),
                subscription::record, subscription::end, null);
        return register(subscription);
    }

    /** Pattern counterpart of {@link #pubSubSubscribe(String)}; the recorded channel is the one published to. */
    @POST
    @Path("/pubsub/psubscribe")
    public String pubSubSubscribeToPatterns(@QueryParam("patterns") String patterns) {
        Subscription subscription = new Subscription();
        subscription.subscriber = pubsub.subscribeToPatterns(List.of(patterns.split(",")),
                subscription::record, subscription::end, null);
        return register(subscription);
    }

    /**
     * Subscribes through the reactive {@code Multi} API. The subscription is established asynchronously, so a test
     * must wait for the channel to show up in {@link #pubSubChannels()} before publishing. Cancelling the stream,
     * via {@link #pubSubUnsubscribe(String, String)}, is what unsubscribes.
     */
    @POST
    @Path("/pubsub/reactive/subscribe")
    public String pubSubSubscribeReactive(@QueryParam("channels") String channels) {
        Subscription subscription = new Subscription();
        subscription.cancellable = reactivePubSub.subscribeAsMessages(channels.split(","))
                .subscribe().with(message -> subscription.record(message.getChannel(), message.getPayload()),
                        ignored -> {
                        }, subscription::end);
        return register(subscription);
    }

    @POST
    @Path("/pubsub/publish/{channel}")
    public void pubSubPublish(@PathParam("channel") String channel, String message) {
        pubsub.publish(channel, message);
    }

    @POST
    @Path("/pubsub/reactive/publish/{channel}")
    public Uni<Void> pubSubPublishReactive(@PathParam("channel") String channel, String message) {
        return reactivePubSub.publish(channel, message);
    }

    @GET
    @Path("/pubsub/subscription/{id}")
    public Map<String, Object> pubSubState(@PathParam("id") String id) {
        Subscription subscription = subscriptions.get(id);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("messages", List.copyOf(subscription.messages));
        state.put("ended", subscription.ended.get());
        state.put("onDuplicatedContext", !subscription.offDuplicatedContext.get());
        return state;
    }

    @DELETE
    @Path("/pubsub/subscription/{id}")
    public void pubSubUnsubscribe(@PathParam("id") String id, @QueryParam("names") String names) {
        Subscription subscription = subscriptions.get(id);
        if (subscription.cancellable != null) {
            subscription.cancellable.cancel();
        } else if (names == null) {
            subscription.subscriber.unsubscribe();
        } else {
            subscription.subscriber.unsubscribe(names.split(","));
        }
    }

    @GET
    @Path("/pubsub/channels")
    public List<String> pubSubChannels() {
        List<String> channels = new ArrayList<>();
        for (Response channel : blocking.execute(Command.PUBSUB, "CHANNELS")) {
            channels.add(channel.toString());
        }
        return channels;
    }

    @GET
    @Path("/pubsub/numpat")
    public int pubSubNumPat() {
        return blocking.execute(Command.PUBSUB, "NUMPAT").toInteger();
    }

    private String register(Subscription subscription) {
        String id = String.valueOf(subscriptionIds.incrementAndGet());
        subscriptions.put(id, subscription);
        return id;
    }

    static final class Subscription {

        final List<String> messages = new CopyOnWriteArrayList<>();
        final AtomicBoolean ended = new AtomicBoolean();
        final AtomicBoolean offDuplicatedContext = new AtomicBoolean();
        volatile PubSubCommands.RedisSubscriber subscriber;
        volatile Cancellable cancellable;

        void record(String channel, String payload) {
            if (!VertxContext.isOnDuplicatedContext()) {
                offDuplicatedContext.set(true);
            }
            messages.add(channel + ":" + payload);
        }

        void end() {
            ended.set(true);
        }

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
    @Path("/with-transaction/cuckoo/{key}")
    public String withTransactionCuckoo(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var c = tx.cuckoo(String.class, String.class);
            c.cfinsertnx(key, "a", "b", "c", "d", "a");
            c.cfadd(key, "x");
            c.cfexists(key, "a");
            c.cfmexists(key, "a", "b", "z");
            c.cfinsert(key, new CfInsertArgs(), "v", "w", "b");
            c.cfadd(key, "a");
            c.cfcount(key, "a");
            c.cfdel(key, "a");
            c.cfcount(key, "a");
        });
        List<Boolean> inserted = result.get(0);
        Object added = result.get(1);
        boolean exists = result.get(2);
        List<Boolean> mexists = result.get(3);
        Object insert = result.get(4);
        long countBefore = result.get(6);
        boolean deleted = result.get(7);
        long countAfter = result.get(8);
        return result.discarded() + ";" + result.size() + ";" + inserted + ";" + (added == null) + ";" + exists + ";"
                + mexists + ";" + (insert == null) + ";" + countBefore + ";" + deleted + ";" + countAfter;
    }

    @POST
    @Path("/with-transaction/reactive/cuckoo/{key}")
    public Uni<String> withTransactionCuckooReactive(@PathParam("key") String key) {
        return reactive.withTransaction(tx -> {
            var c = tx.cuckoo(String.class, String.class);
            return c.cfinsertnx(key, "a", "b", "c")
                    .chain(() -> c.cfadd(key, "a"))
                    .chain(() -> c.cfcount(key, "a"))
                    .chain(() -> c.cfexists(key, "z"));
        }).map(result -> result.discarded() + ";" + result.size() + ";" + result.get(0) + ";" + (result.get(1) == null)
                + ";" + result.get(2) + ";" + result.get(3));
    }

    @POST
    @Path("/with-transaction/topk/{key}")
    public String withTransactionTopK(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var t = tx.topk(String.class, String.class);
            t.topkReserve(key, 3);
            t.topkAdd(key, "a", "a", "b", "b", "a", "c", "a", "b", "c", "d", "d", "d");
            t.topkIncrBy(key, "c", 10);
            Map<String, Integer> couples = new LinkedHashMap<>();
            couples.put("d", 2);
            couples.put("e", 20);
            t.topkIncrBy(key, couples);
            t.topkQuery(key, "a");
            t.topkQuery(key, "a", "b", "e");
            t.topkList(key);
            t.topkListWithCount(key);
        });
        Object reserve = result.get(0);
        List<String> added = result.get(1);
        String evicted = result.get(2);
        Map<String, String> evictedByCouples = result.get(3);
        boolean queried = result.get(4);
        List<Boolean> queriedMany = result.get(5);
        List<String> list = result.get(6);
        Map<String, Integer> withCount = result.get(7);
        return result.discarded() + "," + result.size() + "," + (reserve == null) + "," + added + "," + evicted + ","
                + evictedByCouples + "," + queried + "," + queriedMany + "," + list + "," + withCount;
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

    private static Map<String, String> parsePayload(String body) {
        Map<String, String> payload = new LinkedHashMap<>();
        for (String pair : body.split(",")) {
            String[] fieldAndValue = pair.split("=", 2);
            payload.put(fieldAndValue[0], fieldAndValue[1]);
        }
        return payload;
    }

    private static Map<String, String> parseLastIds(List<String> streams) {
        Map<String, String> lastIds = new LinkedHashMap<>();
        for (String s : streams) {
            String[] keyAndId = s.split(":", 2);
            lastIds.put(keyAndId[0], keyAndId[1]);
        }
        return lastIds;
    }

    /**
     * {@code key:id:field=value,field=value}, the fields sorted, so that the tests can assert on the whole message.
     */
    private static String formatMessage(StreamMessage<String, String, String> message) {
        Map<String, String> payload = message.payload() == null ? Map.of() : new TreeMap<>(message.payload());
        return message.key() + ":" + message.id() + ":" + payload.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    private static List<String> formatMessages(List<StreamMessage<String, String, String>> messages) {
        return messages.stream().map(LettuceBackendResource::formatMessage).toList();
    }

    private static List<String> formatPending(List<PendingMessage> pending) {
        return pending.stream()
                .map(p -> p.getMessageId() + ":" + p.getConsumer() + ":" + p.getDeliveryCount())
                .toList();
    }

    private static String formatPendingSummary(XPendingSummary summary) {
        return summary.getPendingCount() + "," + summary.getLowestId() + "," + summary.getHighestId() + ","
                + new TreeMap<>(summary.getConsumers());
    }

    @POST
    @Path("/stream/add/{key}")
    public String streamAdd(@PathParam("key") String key, @QueryParam("id") String id,
            @QueryParam("maxlen") Long maxlen, @QueryParam("nomkstream") boolean nomkstream, String payload) {
        if (id == null && maxlen == null && !nomkstream) {
            return stream.xadd(key, parsePayload(payload));
        }
        XAddArgs args = new XAddArgs();
        if (id != null) {
            args.id(id);
        }
        if (maxlen != null) {
            args.maxlen(maxlen);
        }
        if (nomkstream) {
            args.nomkstream();
        }
        return stream.xadd(key, args, parsePayload(payload));
    }

    @GET
    @Path("/stream/len/{key}")
    public long streamLen(@PathParam("key") String key) {
        return stream.xlen(key);
    }

    @GET
    @Path("/stream/range/{key}")
    public List<String> streamRange(@PathParam("key") String key, @QueryParam("from") @DefaultValue("-") String from,
            @QueryParam("to") @DefaultValue("+") String to, @QueryParam("count") int count,
            @QueryParam("rev") boolean rev) {
        StreamRange range = StreamRange.of(from, to);
        List<StreamMessage<String, String, String>> messages;
        if (rev) {
            messages = count > 0 ? stream.xrevrange(key, range, count) : stream.xrevrange(key, range);
        } else {
            messages = count > 0 ? stream.xrange(key, range, count) : stream.xrange(key, range);
        }
        return formatMessages(messages);
    }

    @GET
    @Path("/stream/read/{key}")
    public List<String> streamRead(@PathParam("key") String key, @QueryParam("id") @DefaultValue("0-0") String id,
            @QueryParam("count") int count, @QueryParam("block") long block) {
        if (count == 0 && block == 0) {
            return formatMessages(stream.xread(key, id));
        }
        return formatMessages(stream.xread(key, id, readArgs(count, block)));
    }

    @GET
    @Path("/stream/read")
    public List<String> streamReadMultiple(@QueryParam("stream") List<String> streams, @QueryParam("count") int count,
            @QueryParam("block") long block) {
        Map<String, String> lastIds = parseLastIds(streams);
        if (count == 0 && block == 0) {
            return formatMessages(stream.xread(lastIds));
        }
        return formatMessages(stream.xread(lastIds, readArgs(count, block)));
    }

    private static XReadArgs readArgs(int count, long block) {
        XReadArgs args = new XReadArgs();
        if (count > 0) {
            args.count(count);
        }
        if (block > 0) {
            args.block(Duration.ofMillis(block));
        }
        return args;
    }

    @POST
    @Path("/stream/read/blocking/{key}")
    public List<String> streamReadBlocking(@PathParam("key") String key, String payload) {
        CompletableFuture<List<StreamMessage<String, String, String>>> read = reactiveStream
                .xread(key, "0-0", new XReadArgs().block(Duration.ofSeconds(5)))
                .subscribeAsCompletionStage();
        stream.xadd(key, parsePayload(payload));
        return formatMessages(read.join());
    }

    @POST
    @Path("/stream/group/{key}/{group}")
    public void streamGroupCreate(@PathParam("key") String key, @PathParam("group") String group,
            @QueryParam("from") @DefaultValue("$") String from, @QueryParam("mkstream") boolean mkstream) {
        if (mkstream) {
            stream.xgroupCreate(key, group, from, new XGroupCreateArgs().mkstream());
        } else {
            stream.xgroupCreate(key, group, from);
        }
    }

    @DELETE
    @Path("/stream/group/{key}/{group}")
    public boolean streamGroupDestroy(@PathParam("key") String key, @PathParam("group") String group) {
        return stream.xgroupDestroy(key, group);
    }

    @POST
    @Path("/stream/group/{key}/{group}/setid")
    public void streamGroupSetId(@PathParam("key") String key, @PathParam("group") String group,
            @QueryParam("from") String from) {
        stream.xgroupSetId(key, group, from);
    }

    @POST
    @Path("/stream/group/{key}/{group}/consumer/{consumer}")
    public boolean streamGroupCreateConsumer(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer) {
        return stream.xgroupCreateConsumer(key, group, consumer);
    }

    @DELETE
    @Path("/stream/group/{key}/{group}/consumer/{consumer}")
    public long streamGroupDelConsumer(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer) {
        return stream.xgroupDelConsumer(key, group, consumer);
    }

    @GET
    @Path("/stream/readgroup/{key}/{group}/{consumer}")
    public List<String> streamReadGroup(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer, @QueryParam("id") @DefaultValue(">") String id,
            @QueryParam("count") int count, @QueryParam("noack") boolean noack, @QueryParam("block") long block) {
        if (count == 0 && !noack && block == 0) {
            return formatMessages(stream.xreadgroup(group, consumer, key, id));
        }
        return formatMessages(stream.xreadgroup(group, consumer, key, id, readGroupArgs(count, noack, block)));
    }

    @GET
    @Path("/stream/readgroup/{group}/{consumer}")
    public List<String> streamReadGroupMultiple(@PathParam("group") String group,
            @PathParam("consumer") String consumer, @QueryParam("stream") List<String> streams,
            @QueryParam("count") int count, @QueryParam("noack") boolean noack, @QueryParam("block") long block) {
        Map<String, String> lastIds = parseLastIds(streams);
        if (count == 0 && !noack && block == 0) {
            return formatMessages(stream.xreadgroup(group, consumer, lastIds));
        }
        return formatMessages(stream.xreadgroup(group, consumer, lastIds, readGroupArgs(count, noack, block)));
    }

    private static XReadGroupArgs readGroupArgs(int count, boolean noack, long block) {
        XReadGroupArgs args = new XReadGroupArgs();
        if (count > 0) {
            args.count(count);
        }
        if (noack) {
            args.noack();
        }
        if (block > 0) {
            args.block(Duration.ofMillis(block));
        }
        return args;
    }

    @POST
    @Path("/stream/ack/{key}/{group}")
    public int streamAck(@PathParam("key") String key, @PathParam("group") String group, String ids) {
        return stream.xack(key, group, ids.split(","));
    }

    @GET
    @Path("/stream/pending/{key}/{group}")
    public String streamPending(@PathParam("key") String key, @PathParam("group") String group) {
        return formatPendingSummary(stream.xpending(key, group));
    }

    @GET
    @Path("/stream/pending/{key}/{group}/range")
    public List<String> streamPendingRange(@PathParam("key") String key, @PathParam("group") String group,
            @QueryParam("from") @DefaultValue("-") String from, @QueryParam("to") @DefaultValue("+") String to,
            @QueryParam("count") @DefaultValue("10") int count, @QueryParam("consumer") String consumer,
            @QueryParam("idle") long idle) {
        StreamRange range = StreamRange.of(from, to);
        if (consumer == null && idle == 0) {
            return formatPending(stream.xpending(key, group, range, count));
        }
        XPendingArgs args = new XPendingArgs();
        if (consumer != null) {
            args.consumer(consumer);
        }
        if (idle > 0) {
            args.idle(Duration.ofMillis(idle));
        }
        return formatPending(stream.xpending(key, group, range, count, args));
    }

    @POST
    @Path("/stream/claim/{key}/{group}/{consumer}")
    public List<String> streamClaim(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer, @QueryParam("minIdle") long minIdle,
            @QueryParam("justId") boolean justId, String ids) {
        Duration minIdleTime = Duration.ofMillis(minIdle);
        String[] messageIds = ids.split(",");
        if (justId) {
            return formatMessages(stream.xclaim(key, group, consumer, minIdleTime, new XClaimArgs().justId(), messageIds));
        }
        return formatMessages(stream.xclaim(key, group, consumer, minIdleTime, messageIds));
    }

    @POST
    @Path("/stream/autoclaim/{key}/{group}/{consumer}")
    public List<String> streamAutoClaim(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer, @QueryParam("minIdle") long minIdle,
            @QueryParam("start") @DefaultValue("0-0") String start, @QueryParam("count") int count,
            @QueryParam("justId") boolean justId) {
        Duration minIdleTime = Duration.ofMillis(minIdle);
        ClaimedMessages<String, String, String> claimed;
        if (justId) {
            claimed = stream.xautoclaim(key, group, consumer, minIdleTime, start, count, true);
        } else if (count > 0) {
            claimed = stream.xautoclaim(key, group, consumer, minIdleTime, start, count);
        } else {
            claimed = stream.xautoclaim(key, group, consumer, minIdleTime, start);
        }
        List<String> result = new ArrayList<>();
        result.add(claimed.getId());
        result.addAll(formatMessages(claimed.getMessages()));
        return result;
    }

    @POST
    @Path("/stream/trim/{key}")
    public long streamTrim(@PathParam("key") String key, @QueryParam("maxlen") Long maxlen,
            @QueryParam("minid") String minid, @QueryParam("approx") boolean approx) {
        if (maxlen == null && !approx) {
            // the plain threshold is a MINID
            return stream.xtrim(key, minid);
        }
        XTrimArgs args = new XTrimArgs();
        if (maxlen != null) {
            args.maxlen(maxlen);
        }
        if (minid != null) {
            args.minid(minid);
        }
        if (approx) {
            args.nearlyExactTrimming();
        }
        return stream.xtrim(key, args);
    }

    @DELETE
    @Path("/stream/{key}")
    public int streamDel(@PathParam("key") String key, @QueryParam("id") List<String> ids) {
        return stream.xdel(key, ids.toArray(new String[0]));
    }

    @POST
    @Path("/stream/reactive/add/{key}")
    public Uni<String> streamAddReactive(@PathParam("key") String key, @QueryParam("id") String id, String payload) {
        if (id == null) {
            return reactiveStream.xadd(key, parsePayload(payload));
        }
        return reactiveStream.xadd(key, new XAddArgs().id(id), parsePayload(payload));
    }

    @GET
    @Path("/stream/reactive/len/{key}")
    public Uni<Long> streamLenReactive(@PathParam("key") String key) {
        return reactiveStream.xlen(key);
    }

    @GET
    @Path("/stream/reactive/range/{key}")
    public Uni<List<String>> streamRangeReactive(@PathParam("key") String key) {
        return reactiveStream.xrange(key, StreamRange.of("-", "+")).map(LettuceBackendResource::formatMessages);
    }

    @GET
    @Path("/stream/reactive/read/{key}")
    public Uni<List<String>> streamReadReactive(@PathParam("key") String key,
            @QueryParam("id") @DefaultValue("0-0") String id, @QueryParam("block") long block) {
        Uni<List<StreamMessage<String, String, String>>> read = block > 0
                ? reactiveStream.xread(key, id, new XReadArgs().block(Duration.ofMillis(block)))
                : reactiveStream.xread(key, id);
        return read.map(LettuceBackendResource::formatMessages);
    }

    @GET
    @Path("/stream/reactive/readgroup/{key}/{group}/{consumer}")
    public Uni<List<String>> streamReadGroupReactive(@PathParam("key") String key, @PathParam("group") String group,
            @PathParam("consumer") String consumer) {
        return reactiveStream.xreadgroup(group, consumer, key, ">").map(LettuceBackendResource::formatMessages);
    }

    @POST
    @Path("/stream/reactive/ack/{key}/{group}")
    public Uni<Integer> streamAckReactive(@PathParam("key") String key, @PathParam("group") String group, String ids) {
        return reactiveStream.xack(key, group, ids.split(","));
    }

    @GET
    @Path("/stream/reactive/pending/{key}/{group}")
    public Uni<String> streamPendingReactive(@PathParam("key") String key, @PathParam("group") String group) {
        return reactiveStream.xpending(key, group).map(LettuceBackendResource::formatPendingSummary);
    }

    @POST
    @Path("/with-transaction/stream/{key}")
    public String withTransactionStream(@PathParam("key") String key) {
        Map<String, String> payload = Map.of("a", "1");
        TransactionResult result = blocking.withTransaction(tx -> {
            var s = tx.stream(String.class);
            s.xgroupCreate(key, "g1", "$", new XGroupCreateArgs().mkstream());
            s.xadd(key, new XAddArgs().id("1-0"), payload);
            s.xadd(key, new XAddArgs().id("2-0"), payload);
            s.xlen(key);
            s.xrange(key, StreamRange.of("-", "+"));
            // inside MULTI a blocking read does not block
            s.xread(key, "0-0", new XReadArgs().block(Duration.ofSeconds(10)));
            s.xreadgroup("g1", "c1", key, ">");
            s.xpending(key, "g1");
            s.xack(key, "g1", "1-0");
            s.xdel(key, "0-1");
            s.xtrim(key, new XTrimArgs().maxlen(1));
        });
        Object created = result.get(0);
        String id1 = result.get(1);
        String id2 = result.get(2);
        long length = result.get(3);
        List<StreamMessage<String, String, String>> range = result.get(4);
        List<StreamMessage<String, String, String>> read = result.get(5);
        List<StreamMessage<String, String, String>> group = result.get(6);
        XPendingSummary pending = result.get(7);
        int acknowledged = result.get(8);
        int deleted = result.get(9);
        long trimmed = result.get(10);
        return result.discarded() + "," + result.size() + "," + (created == null) + "," + id1 + "," + id2 + "," + length
                + "," + formatMessages(range) + "," + formatMessages(read) + "," + formatMessages(group) + ","
                + formatPendingSummary(pending) + "," + acknowledged + "," + deleted + "," + trimmed;
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
        for (FieldSpec field : parseFields(fields)) {
            if (field.alias() == null) {
                args.indexedField(field.name(), field.type(), field.options());
            } else {
                args.indexedField(field.name(), field.alias(), field.type(), field.options());
            }
        }
        search.ftCreate(index, args);
    }

    private record FieldSpec(String name, String alias, FieldType type, FieldOptions options) {

        IndexedField toIndexedField() {
            return alias == null ? IndexedField.from(name, type, options) : IndexedField.from(name, alias, type, options);
        }

    }

    private static List<FieldSpec> parseFields(String spec) {
        List<FieldSpec> fields = new ArrayList<>();
        for (String token : spec.split(",")) {
            String[] parts = token.trim().split(":");
            String[] name = parts[0].split("=");
            FieldType type = FieldType.valueOf(parts[1].toUpperCase());
            FieldOptions options = new FieldOptions();
            if (parts.length > 2 && parts[2].equalsIgnoreCase("sortable")) {
                options.sortable();
            }
            fields.add(new FieldSpec(name[0], name.length > 1 ? name[1] : null, type, options));
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
        search.ftAlter(index, parseFields(field).get(0).toIndexedField());
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

    @POST
    @Path("/with-transaction/bloom/{key}")
    public String withTransactionBloom(@PathParam("key") String key) {
        TransactionResult result = blocking.withTransaction(tx -> {
            var b = tx.bloom(String.class, String.class);
            b.bfmadd(key, "a", "b", "c", "d", "a");
            b.bfadd(key, "x");
            b.bfexists(key, "a");
            b.bfmexists(key, "a", "b", "z");
            b.bfinsert(key, new BfInsertArgs(), "v", "w", "b");
        });
        List<Boolean> madd = result.get(0);
        boolean added = result.get(1);
        boolean exists = result.get(2);
        List<Boolean> mexists = result.get(3);
        List<Boolean> inserted = result.get(4);
        return result.discarded() + ";" + result.size() + ";" + madd + ";" + added + ";" + exists + ";" + mexists + ";"
                + inserted;
    }

    @POST
    @Path("/with-transaction/reactive/bloom/{key}")
    public Uni<String> withTransactionBloomReactive(@PathParam("key") String key) {
        return reactive.withTransaction(tx -> {
            var b = tx.bloom(String.class, String.class);
            return b.bfmadd(key, "a", "b", "c")
                    .chain(() -> b.bfexists(key, "a"))
                    .chain(() -> b.bfexists(key, "z"));
        }).map(result -> result.discarded() + ";" + result.size() + ";" + result.get(0) + ";" + result.get(1) + ";"
                + result.get(2));
    }

}
