package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ClaimedMessages;
import io.quarkus.redis.datasource.stream.PendingMessage;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.StreamRange;
import io.quarkus.redis.datasource.stream.XAddArgs;
import io.quarkus.redis.datasource.stream.XClaimArgs;
import io.quarkus.redis.datasource.stream.XGroupCreateArgs;
import io.quarkus.redis.datasource.stream.XGroupSetIdArgs;
import io.quarkus.redis.datasource.stream.XPendingArgs;
import io.quarkus.redis.datasource.stream.XPendingSummary;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.quarkus.redis.datasource.stream.XReadGroupArgs;
import io.quarkus.redis.datasource.stream.XTrimArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class ReactiveStreamCommandsImpl<K, F, V> extends AbstractStreamCommands<K, F, V>
        implements ReactiveStreamCommands<K, F, V>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;
    private final Type typeOfValue;
    private final Type typeOfField;
    private final Type typeOfKey;

    public ReactiveStreamCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type f, Type v) {
        super(redis, k, f, v);
        this.typeOfKey = k;
        this.typeOfField = f;
        this.typeOfValue = v;
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Integer> xack(K key, String group, String... ids) {
        return super._xack(key, group, ids)
                .map(Response::toInteger);
    }

    @Override
    public Uni<String> xadd(K key, Map<F, V> payload) {
        return super._xadd(key, payload)
                .map(ReactiveStreamCommandsImpl::getIdOrNull);
    }

    protected static String getIdOrNull(Response r) {
        if (r == null) {
            return null;
        }
        return r.toString();
    }

    @Override
    public Uni<String> xadd(K key, XAddArgs args, Map<F, V> payload) {
        return super._xadd(key, args, payload)
                .map(ReactiveStreamCommandsImpl::getIdOrNull);
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count) {
        return super._xautoclaim(key, group, consumer, minIdleTime, start, count)
                .map(r -> decodeAsClaimedMessages(key, r));
    }

    protected ClaimedMessages<K, F, V> decodeAsClaimedMessages(K key, Response r) {
        if (r == null) {
            return new ClaimedMessages<>(null, List.of());
        }
        var id = r.get(0).toString(); // This is the stream id for the next auto-claim call
        var l = r.get(1);
        var list = decodeListOfMessages(key, l);
        // ignore the third item - competing messages

        return new ClaimedMessages<>(id, list);
    }

    protected List<StreamMessage<K, F, V>> decodeMessageListPrefixedByKey(Response r) {
        // Each response is a _list_ of two elements where the first element is the key.
        // The second element is the list of messages
        if (r == null) {
            return List.of();
        }
        K actualKey = marshaller.decode(typeOfKey, r.get(0));
        var listOfMessages = r.get(1);
        List<StreamMessage<K, F, V>> list = new ArrayList<>();
        for (int i = 0; i < listOfMessages.size(); i++) {
            list.add(decodeMessageWithStreamId(actualKey, listOfMessages.get(i)));
        }
        return list;
    }

    private StreamMessage<K, F, V> decodeMessageWithStreamId(K key, Response response) {

        // the response is an array with two elements:
        // 1. the stream id
        // 2. the payload (another array)

        if (response == null) {
            return null;
        }

        if (response.type() == ResponseType.BULK) {
            // JUSTID was used
            return new StreamMessage<>(key, response.toString(), Map.of());
        } else {
            var streamId = response.get(0).toString();
            var payload = response.get(1);
            var content = decodeMessagePayload(payload);
            return new StreamMessage<>(key, streamId, content);
        }
    }

    Map<F, V> decodeMessagePayload(Response response) {
        Map<F, V> map = new HashMap<>();
        F current = null;
        for (Response nested : response) {
            if (current == null) {
                current = marshaller.decode(typeOfField, nested);
            } else {
                map.put(current, marshaller.decode(typeOfValue, nested));
                current = null;
            }
        }
        return map;
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start) {
        return super._xautoclaim(key, group, consumer, minIdleTime, start)
                .map(r -> decodeAsClaimedMessages(key, r));
    }

    @Override
    public Uni<ClaimedMessages<K, F, V>> xautoclaim(K key, String group, String consumer, Duration minIdleTime, String start,
            int count, boolean justId) {
        return super._xautoclaim(key, group, consumer, minIdleTime, start, count, justId)
                .map(r -> decodeAsClaimedMessages(key, r));
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xclaim(K key, String group, String consumer, Duration minIdleTime, String... id) {
        return super._xclaim(key, group, consumer, minIdleTime, id)
                .map(r -> decodeListOfMessages(key, r));
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xclaim(K key, String group, String consumer, Duration minIdleTime, XClaimArgs args,
            String... id) {
        return super._xclaim(key, group, consumer, minIdleTime, args, id)
                .map(r -> decodeListOfMessages(key, r));
    }

    @Override
    public Uni<Integer> xdel(K key, String... id) {
        return super._xdel(key, id)
                .map(Response::toInteger);
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from) {
        return super._xgroupCreate(key, groupname, from)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupCreate(K key, String groupname, String from, XGroupCreateArgs args) {
        return super._xgroupCreate(key, groupname, from, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Boolean> xgroupCreateConsumer(K key, String groupname, String consumername) {
        return super._xgroupCreateConsumer(key, groupname, consumername)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Long> xgroupDelConsumer(K key, String groupname, String consumername) {
        return super._xgroupDelConsumer(key, groupname, consumername)
                .map(Response::toLong);
    }

    @Override
    public Uni<Boolean> xgroupDestroy(K key, String groupname) {
        return super._xgroupDestroy(key, groupname)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from) {
        return super._xgroupSetId(key, groupname, from)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> xgroupSetId(K key, String groupname, String from, XGroupSetIdArgs args) {
        return super._xgroupSetId(key, groupname, from, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Long> xlen(K key) {
        return super._xlen(key)
                .map(Response::toLong);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrange(K key, StreamRange range, int count) {
        return super._xrange(key, range, count)
                .map(r -> decodeListOfMessages(key, r));
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrange(K key, StreamRange range) {
        return super._xrange(key, range)
                .map(r -> decodeListOfMessages(key, r));
    }

    protected List<StreamMessage<K, F, V>> decodeListOfMessages(K key, Response r) {
        if (r == null) {
            return List.of();
        }
        // The response is a list.
        // Each element is a list of two element (stream id, payload)
        List<StreamMessage<K, F, V>> list = new ArrayList<>();
        for (Response response : r) {
            list.add(decodeMessageWithStreamId(key, response));
        }
        return list;
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(K key, String id) {
        return xread(Map.of(key, id));
    }

    protected List<StreamMessage<K, F, V>> decodeAsListOfMessagesFromXRead(Response r) {
        if (r == null) {
            return List.of();
        }
        // The response is a _map_ key -> list, in this case
        List<StreamMessage<K, F, V>> list = new ArrayList<>();
        for (Response response : r) {
            // Each response is a _list_ where the first element is the key.
            // The other elements are a list of array (stream id, message)
            list.addAll(decodeMessageListPrefixedByKey(response));
        }
        return list;
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(Map<K, String> lastIdsPerStream) {
        return super._xread(lastIdsPerStream)
                .map(this::decodeAsListOfMessagesFromXRead);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(K key, String id, XReadArgs args) {
        return xread(Map.of(key, id), args);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xread(Map<K, String> lastIdsPerStream, XReadArgs args) {
        return super._xread(lastIdsPerStream, args)
                .map(this::decodeAsListOfMessagesFromXRead);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, K key, String id) {
        return xreadgroup(group, consumer, Map.of(key, id));

    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream) {
        return super._xreadgroup(group, consumer, lastIdsPerStream)
                .map(this::decodeAsListOfMessagesFromXRead);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, K key, String id, XReadGroupArgs args) {
        return xreadgroup(group, consumer, Map.of(key, id), args);

    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xreadgroup(String group, String consumer, Map<K, String> lastIdsPerStream,
            XReadGroupArgs args) {
        return super._xreadgroup(group, consumer, lastIdsPerStream, args)
                .map(this::decodeAsListOfMessagesFromXRead);
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrevrange(K key, StreamRange range, int count) {
        return super._xrevrange(key, range, count)
                .map(r -> decodeListOfMessages(key, r));
    }

    @Override
    public Uni<List<StreamMessage<K, F, V>>> xrevrange(K key, StreamRange range) {
        return super._xrevrange(key, range)
                .map(r -> decodeListOfMessages(key, r));
    }

    @Override
    public Uni<Long> xtrim(K key, String threshold) {
        return super._xtrim(key, new XTrimArgs().minid(threshold))
                .map(Response::toLong);
    }

    @Override
    public Uni<Long> xtrim(K key, XTrimArgs args) {
        return super._xtrim(key, args)
                .map(Response::toLong);
    }

    @Override
    public Uni<XPendingSummary> xpending(K key, String group) {
        return super._xpending(key, group)
                .map(this::decodeAsXPendingSummary);
    }

    @Override
    public Uni<List<PendingMessage>> xpending(K key, String group, StreamRange range, int count) {
        return xpending(key, group, range, count, null);
    }

    @Override
    public Uni<List<PendingMessage>> xpending(K key, String group, StreamRange range, int count, XPendingArgs args) {
        return super._xpending(key, group, range, count, args)
                .map(this::decodeListOfPendingMessages);
    }

    protected List<PendingMessage> decodeListOfPendingMessages(Response r) {
        if (r == null) {
            return List.of();
        }
        List<PendingMessage> list = new ArrayList<>();
        for (Response response : r) {
            var id = response.get(0).toString();
            var name = response.get(1).toString();
            var dur = Duration.ofMillis(response.get(2).toLong());
            var count = response.get(3).toInteger();
            list.add(new PendingMessage(id, name, dur, count));
        }
        return list;
    }

    protected XPendingSummary decodeAsXPendingSummary(Response r) {
        if (r == null) {
            return null;
        }

        var pending = r.get(0).toLong();
        var lowest = r.get(1) != null ? r.get(1).toString() : null;
        var highest = r.get(2) != null ? r.get(2).toString() : null;

        Map<String, Long> consumers = new HashMap<>();
        if (r.get(3) != null) {
            for (Response nested : r.get(3)) {
                consumers.put(nested.get(0).toString(), nested.get(1).toLong());
            }
        }

        return new XPendingSummary(pending, lowest, highest, consumers);
    }
}
