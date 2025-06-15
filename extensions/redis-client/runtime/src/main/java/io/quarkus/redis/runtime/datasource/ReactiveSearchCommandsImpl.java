package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.AggregateDocument;
import io.quarkus.redis.datasource.search.AggregationResponse;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.Document;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.search.SearchQueryResponse;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.search.SpellCheckResponse;
import io.quarkus.redis.datasource.search.SynDumpResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class ReactiveSearchCommandsImpl<K> extends AbstractSearchCommands<K>
        implements ReactiveSearchCommands<K>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;
    protected final Type keyType;

    public ReactiveSearchCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.reactive = redis;
        this.keyType = k;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<List<K>> ft_list() {
        return super._ft_list().map(r -> marshaller.decodeAsList(r, keyType));
    }

    @Override
    public Uni<AggregationResponse> ftAggregate(String indexName, String query, AggregateArgs args) {
        return super._ftAggregate(indexName, query, args).map(r -> decodeAggregateResponse(r, args.hasCursor()));
    }

    AggregationResponse decodeAggregateResponse(Response response, boolean cursor) {
        if (response == null) {
            return new AggregationResponse(Collections.emptyList());
        }

        var payload = response;
        var cursorId = -1L;
        if (cursor) {
            payload = response.get(0);
            cursorId = response.get(1).toLong();
        }

        if (isMap(payload)) {
            // Redis 7.2+ behavior
            Response results = payload.get("results");
            List<AggregateDocument> docs = new ArrayList<>();
            if (results != null) {
                for (Response result : results) {
                    Map<String, Document.Property> list = new HashMap<>();
                    if (result.containsKey("extra_attributes")) {
                        Response attributes = result.get("extra_attributes");
                        for (String propertyName : attributes.getKeys()) {
                            list.put(propertyName, new Document.Property(propertyName, attributes.get(propertyName)));
                        }
                    }
                    AggregateDocument doc = new AggregateDocument(list);
                    docs.add(doc);
                }
            }
            return new AggregationResponse(cursorId, docs);
        }

        // Pre-Redis 7.2 behavior:
        List<AggregateDocument> docs = new ArrayList<>();
        for (int i = 1; i < payload.size(); i++) {
            var nested = payload.get(i);
            String propertyName = null;
            Map<String, Document.Property> list = new HashMap<>();
            for (Response n : nested) {
                if (propertyName == null) {
                    propertyName = n.toString();
                } else {
                    list.put(propertyName, new Document.Property(propertyName, n));
                    propertyName = null;
                }
            }
            AggregateDocument doc = new AggregateDocument(list);
            docs.add(doc);
        }
        return new AggregationResponse(cursorId, docs);
    }

    @Override
    public Uni<AggregationResponse> ftAggregate(String indexName, String query) {
        return super._ftAggregate(indexName, query).map(r -> decodeAggregateResponse(r, false));
    }

    @Override
    public Uni<Void> ftAliasAdd(String alias, String index) {
        return super._ftAliasAdd(alias, index).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAliasDel(String alias) {
        return super._ftAliasDel(alias).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAliasUpdate(String alias, String index) {
        return super._ftAliasUpdate(alias, index).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        return super._ftAlter(index, field, skipInitialScan).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftCreate(String index, CreateArgs args) {
        return super._ftCreate(index, args).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftCursorDel(String index, long cursor) {
        return super._ftCursorDel(index, cursor).replaceWithVoid();
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor) {
        return super._ftCursorRead(index, cursor).map(r -> decodeAggregateResponse(r, true));
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor, int count) {
        return super._ftCursorRead(index, cursor, count).map(r -> decodeAggregateResponse(r, true));
    }

    @Override
    public Uni<Void> ftDropIndex(String index) {
        return super._ftDropIndex(index).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDropIndex(String index, boolean dd) {
        return super._ftDropIndex(index, dd).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDictAdd(String dict, String... words) {
        return super._ftDictAdd(dict, words).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDictDel(String dict, String... words) {
        return super._ftDictDel(dict, words).replaceWithVoid();
    }

    @Override
    public Uni<List<String>> ftDictDump(String dict) {
        return super._ftDictDump(dict).map(r -> marshaller.decodeAsList(r, String.class));
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query, QueryArgs args) {
        return super._ftSearch(index, query, args).map(r -> decodeSearchQueryResult(r, args));
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query) {
        return super._ftSearch(index, query).map(r -> decodeSearchQueryResult(r, null));
    }

    SearchQueryResponse decodeSearchQueryResult(Response response, QueryArgs args) {
        if (response == null) {
            return new SearchQueryResponse(0, Collections.emptyList());
        }

        if (isMap(response)) {
            // Read it as a map - Redis 7.2 behavior
            var count = response.get("total_results");
            if (count == null || count.toInteger() == 0) {
                return new SearchQueryResponse(0, Collections.emptyList());
            }
            var total = count.toInteger();

            List<Document> docs = new ArrayList<>();
            Response results = response.get("results");
            for (int i = 0; i < results.size(); i++) {
                Response result = results.get(i);
                Response score = result.get("score");
                Response payload = result.get("payload");
                Response doc = result.get("extra_attributes");

                Map<String, Document.Property> properties = new HashMap<>();
                if (doc != null) {
                    for (String k : doc.getKeys()) {
                        properties.put(k, new Document.Property(k, doc.get(k)));
                    }
                }
                docs.add(new Document(result.get("id").toString(), score == null ? 1.0 : score.toDouble(), payload,
                        properties));
            }

            return new SearchQueryResponse(total, docs);

        }

        // 7.2- behavior
        var count = response.get(0).toInteger();
        if (count == 0) {
            return new SearchQueryResponse(0, Collections.emptyList());
        }

        List<Document> docs = new ArrayList<>();
        for (int i = 1; i < response.size();) {
            var offset = i;
            Response key = response.get(offset);

            Response score = null;
            if (args != null && args.containsScore()) {
                offset++;
                score = response.get(offset);
            }
            Response payload = null;
            if (args != null && args.containsPayload()) {
                offset++;
                payload = response.get(offset);
            }
            if (args != null && args.containsSortKeys()) {
                offset++;
            }

            Response doc = null;
            if (args == null || !args.nocontent) {
                offset++;
                doc = response.get(offset);
            }

            String att = null;
            Map<String, Document.Property> properties = new HashMap<>();
            if (doc != null) {
                for (Response nested : doc) {
                    if (att == null) {
                        att = nested.toString();
                    } else {
                        Document.Property property = new Document.Property(att, nested);
                        properties.put(att, property);
                        att = null;
                    }
                }
            }
            docs.add(new Document(key.toString(), score == null ? 1.0 : score.toDouble(), payload, properties));

            // Increment
            i = offset + 1;
        }

        return new SearchQueryResponse(count, docs);
    }

    @Override
    public Uni<SpellCheckResponse> ftSpellCheck(String index, String query) {
        return super._ftSpellCheck(index, query).map(this::decodeSpellcheckResponse);
    }

    SpellCheckResponse decodeSpellcheckResponse(Response response) {
        if (response == null || response.size() == 0) {
            return new SpellCheckResponse(Collections.emptyMap());
        }
        Map<String, List<SpellCheckResponse.SpellCheckSuggestion>> resp = new LinkedHashMap<>();
        if (isMap(response)) {
            // Redis 7.2 behavior.
            Response results = response.get("results");
            Set<String> terms = results.getKeys();
            for (String word : terms) {
                List<SpellCheckResponse.SpellCheckSuggestion> list = new ArrayList<>();
                Response suggestions = results.get(word);
                if (suggestions.size() == 0) {
                    resp.put(word, Collections.emptyList());
                } else {
                    for (Response suggestion : suggestions) {
                        for (String proposal : suggestion.getKeys()) {
                            double distance = suggestion.get(proposal).toDouble();
                            if (!proposal.equals(word)) {
                                list.add(new SpellCheckResponse.SpellCheckSuggestion(proposal, distance));
                            }
                        }
                        if (!list.isEmpty()) {
                            resp.put(word, list);
                        }
                    }
                }

            }
            return new SpellCheckResponse(resp);
        }

        for (Response term : response) {
            if (!term.get(0).toString().equals("TERM")) {
                continue; // Unknown format
            } else {
                String word = term.get(1).toString();
                Response suggestions = term.get(2);
                if (suggestions.size() == 0) {
                    resp.put(word, Collections.emptyList());
                } else {
                    List<SpellCheckResponse.SpellCheckSuggestion> list = new ArrayList<>();
                    for (Response suggestion : suggestions) {
                        double distance = suggestion.get(0).toDouble();
                        String proposal = suggestion.get(1).toString();
                        if (!proposal.equals(word)) {
                            list.add(new SpellCheckResponse.SpellCheckSuggestion(proposal, distance));
                        }
                    }
                    if (!list.isEmpty()) {
                        resp.put(word, list);
                    }
                }
            }
        }
        return new SpellCheckResponse(resp);
    }

    @Override
    public Uni<SpellCheckResponse> ftSpellCheck(String index, String query, SpellCheckArgs args) {
        return super._ftSpellCheck(index, query, args).map(this::decodeSpellcheckResponse);
    }

    @Override
    public Uni<SynDumpResponse> ftSynDump(String index) {
        return super._ftSynDump(index).map(this::decodeSynDumpResponse);
    }

    SynDumpResponse decodeSynDumpResponse(Response r) {
        if (r == null || r.size() == 0) {
            return new SynDumpResponse(Collections.emptyMap());
        }
        if (isMap(r)) {
            Set<String> keys = r.getKeys();
            // Redis 7.2 behavior
            Map<String, List<String>> synonyms = new HashMap<>();
            for (String key : keys) {
                var groups = r.get(key);
                for (Response group : groups) {
                    synonyms.computeIfAbsent(group.toString(), x -> new ArrayList<>()).add(key);
                }
            }
            return new SynDumpResponse(synonyms);

        }

        Map<String, List<String>> synonyms = new HashMap<>();
        String term = null;
        for (Response response : r) {
            if (response.type() == ResponseType.BULK) {
                term = response.toString();
            } else if (response.type() == ResponseType.MULTI) {
                for (Response group : response) {
                    synonyms.computeIfAbsent(group.toString(), x -> new ArrayList<>()).add(term);
                }
            }
        }
        return new SynDumpResponse(synonyms);
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, String... words) {
        return super._ftSynUpdate(index, groupId, words).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        return super._ftSynUpdate(index, groupId, skipInitialScan, words).replaceWithVoid();
    }

    @Override
    public Uni<Set<String>> ftTagVals(String index, String field) {
        return super._ftTagVals(index, field).map(r -> marshaller.decodeAsSet(r, String.class));
    }
}
