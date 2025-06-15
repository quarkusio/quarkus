package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.graph.GraphQueryResponseItem;
import io.quarkus.redis.datasource.graph.ReactiveGraphCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class ReactiveGraphCommandsImpl<K> extends AbstractGraphCommands<K>
        implements ReactiveGraphCommands<K>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;
    protected final Class<K> typeOfKey;

    public ReactiveGraphCommandsImpl(ReactiveRedisDataSourceImpl redis, Class<K> k) {
        super(redis, k);
        this.typeOfKey = k;
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Void> graphDelete(K key) {
        return super._graphDelete(key).replaceWithVoid();
    }

    @Override
    public Uni<String> graphExplain(K key, String query) {
        return super._graphExplain(key, query).map(Response::toString); // TODO Check result format
    }

    @Override
    public Uni<List<K>> graphList() {
        return super._graphList().map(r -> marshaller.decodeAsList(r, typeOfKey));
    }

    @Override
    public Uni<List<Map<String, GraphQueryResponseItem>>> graphQuery(K key, String query) {
        return super._graphQuery(key, query).map(ReactiveGraphCommandsImpl::decodeQueryResponse);
    }

    @Override
    public Uni<List<Map<String, GraphQueryResponseItem>>> graphQuery(K key, String query, Duration timeout) {
        return super._graphQuery(key, query, timeout).map(ReactiveGraphCommandsImpl::decodeQueryResponse);
    }

    static List<Map<String, GraphQueryResponseItem>> decodeQueryResponse(Response r) {
        if (r.type() == ResponseType.MULTI) {
            if (r.size() <= 1) {
                // No response
                return Collections.emptyList();
            } else {
                Response keys = r.get(0);
                Response values = r.get(1);
                List<Map<String, GraphQueryResponseItem>> results = new ArrayList<>();
                for (Response match : values) {
                    Map<String, GraphQueryResponseItem> result = new LinkedHashMap<>();
                    for (int i = 0; i < keys.size(); i++) {
                        Response val = match.get(i);
                        String key = keys.get(i).toString();
                        result.put(key, decodeItem(key, val));
                    }
                    results.add(result);
                }
                return results;
            }
        }
        return Collections.emptyList();
    }

    private static boolean isNode(Response multi) {
        if (multi == null || multi.type() != ResponseType.MULTI) {
            return false;
        }
        boolean idFound = false;
        boolean labelsFound = false;
        for (Response response : multi) {
            if ("id".equalsIgnoreCase(response.get(0).toString())) {
                idFound = true;
            } else if ("labels".equalsIgnoreCase(response.get(0).toString())) {
                labelsFound = true;
            }
        }
        return idFound && labelsFound;
    }

    private static boolean isRelation(Response multi) {
        if (multi == null || multi.type() != ResponseType.MULTI) {
            return false;
        }
        boolean idFound = false;
        boolean typeFound = false;
        for (Response response : multi) {
            if ("id".equalsIgnoreCase(response.get(0).toString())) {
                idFound = true;
            } else if ("type".equalsIgnoreCase(response.get(0).toString())) {
                typeFound = true;
            }
        }
        return idFound && typeFound;
    }

    private static GraphQueryResponseItem decodeItem(String key, Response val) {
        if (isNode(val)) {
            return decodeNode(key, val);
        } else if (isRelation(val)) {
            return decodeRelation(key, val);
        } else {
            return decodeScalar(key, val);
        }
    }

    private static GraphQueryResponseItem.NodeItem decodeNode(String key, Response val) {
        return new NodeImpl(key, val);
    }

    private static GraphQueryResponseItem.RelationItem decodeRelation(String key, Response val) {

        return new RelationImpl(key, val);
    }

    private static GraphQueryResponseItem.ScalarItem decodeScalar(String key, Response val) {
        return new ScalarImpl(key, val);
    }

    private static class NodeImpl implements GraphQueryResponseItem.NodeItem {

        private final String key;
        private final Map<String, Response> map;
        private final List<ScalarItem> items;

        public NodeImpl(String key, Response val) {
            this.key = key;
            // We know that val is of type MULTI
            this.map = new HashMap<>();
            for (Response response : val) {
                map.put(response.get(0).toString(), response.get(1));
            }
            Response response = map.get("properties");
            this.items = new ArrayList<>();
            for (Response nested : response) {
                items.add(decodeScalar(nested.get(0).toString(), nested.get(1)));
            }
        }

        @Override
        public String toString() {
            return "Node{" + "key='" + key + '\'' + ", items=" + items + '}';
        }

        @Override
        public String name() {
            return key;
        }

        @Override
        public long id() {
            return map.get("id").toLong();
        }

        @Override
        public List<String> labels() {
            List<String> res = new ArrayList<>();
            for (Response labels : map.get("labels")) {
                res.add(labels.toString());
            }
            return res;
        }

        @Override
        public List<ScalarItem> properties() {
            return items;
        }

        @Override
        public ScalarItem get(String property) {
            for (ScalarItem item : items) {
                if (item.name().equals(property)) {
                    return item;
                }
            }
            return null;
        }
    }

    private static class ScalarImpl implements GraphQueryResponseItem.ScalarItem {
        private final String key;
        private final Response val;

        public ScalarImpl(String key, Response val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public String toString() {
            return "Scalar{" + "key='" + key + '\'' + ", val=" + val + '}';
        }

        @Override
        public String name() {
            return key;
        }

        @Override
        public boolean asBoolean() {
            return val.toBoolean();
        }

        @Override
        public int asInteger() {
            return val.toInteger();
        }

        @Override
        public double asDouble() {
            return val.toDouble();
        }

        @Override
        public boolean isNull() {
            return val == null;
        }

        @Override
        public String asString() {
            return val.toString();
        }
    }

    private static class RelationImpl implements GraphQueryResponseItem.RelationItem {

        private final String key;
        private final Map<String, Response> map;
        private final List<ScalarItem> items;

        public RelationImpl(String key, Response val) {
            this.key = key;
            // We know that val is of type MULTI
            this.map = new HashMap<>();
            for (Response response : val) {
                map.put(response.get(0).toString(), response.get(1));
            }
            Response response = map.get("properties");
            this.items = new ArrayList<>();
            for (Response nested : response) {
                items.add(decodeScalar(nested.get(0).toString(), nested.get(1)));
            }
        }

        @Override
        public String toString() {
            return "Relation{" + "key='" + key + '\'' + ", items=" + items + '}';
        }

        @Override
        public String name() {
            return key;
        }

        @Override
        public long id() {
            return map.get("id").toLong();
        }

        @Override
        public String type() {
            return map.get("type").toString();
        }

        @Override
        public long source() {
            return map.get("src_node").toLong();
        }

        @Override
        public long destination() {
            return map.get("dest_node").toLong();
        }

        @Override
        public List<ScalarItem> properties() {
            return items;
        }

        @Override
        public ScalarItem get(String property) {
            for (ScalarItem item : items) {
                if (item.name().equals(property)) {
                    return item;
                }
            }
            return null;
        }
    }
}
