package io.quarkus.redis.lettuce.runtime.internal.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.FieldValue;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SynUpdateArgs;
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
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;

public class LettuceReactiveSearchCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveSearchCommands<K> {

    private final ReactiveRedisDataSource dataSource;
    private final LettuceSearchCursors cursors;

    public LettuceReactiveSearchCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
        this.cursors = connection.searchCursors();
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<List<K>> ft_list() {
        return _ft_list().toUni();
    }

    LettuceCommand<List<String>, List<K>> _ft_list() {
        return LettuceCommand.of(async::ftList, this::decodeIndexNames);
    }

    @Override
    public Uni<AggregationResponse> ftAggregate(String indexName, String query, AggregateArgs args) {
        return _ftAggregate(indexName, query, args).toUni();
    }

    LettuceCommand<AggregationReply<byte[]>, AggregationResponse> _ftAggregate(String indexName, String query,
            AggregateArgs args) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        io.lettuce.core.search.arguments.AggregateArgs lettuceArgs = LettuceSearchCommandsConverters
                .toLettuceAggregateArgs(args);
        return LettuceCommand.of(() -> async.ftAggregate(indexName, query, lettuceArgs),
                reply -> decodeAggregationReply(indexName, 0, args.getCursorMaxIdle(), reply));
    }

    @Override
    public Uni<AggregationResponse> ftAggregate(String indexName, String query) {
        return _ftAggregate(indexName, query).toUni();
    }

    LettuceCommand<AggregationReply<byte[]>, AggregationResponse> _ftAggregate(String indexName, String query) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        return LettuceCommand.of(() -> async.ftAggregate(indexName, query),
                reply -> decodeAggregationReply(indexName, 0, reply));
    }

    @Override
    public Uni<Void> ftAliasAdd(String alias, String index) {
        return _ftAliasAdd(alias, index).toUni();
    }

    LettuceCommand<String, Void> _ftAliasAdd(String alias, String index) {
        notNullOrBlank(alias, "alias");
        notNullOrBlank(index, "index");
        return LettuceCommand.discarding(() -> async.ftAliasadd(alias, index));
    }

    @Override
    public Uni<Void> ftAliasDel(String alias) {
        return _ftAliasDel(alias).toUni();
    }

    LettuceCommand<String, Void> _ftAliasDel(String alias) {
        notNullOrBlank(alias, "alias");
        return LettuceCommand.discarding(() -> async.ftAliasdel(alias));
    }

    @Override
    public Uni<Void> ftAliasUpdate(String alias, String index) {
        return _ftAliasUpdate(alias, index).toUni();
    }

    LettuceCommand<String, Void> _ftAliasUpdate(String alias, String index) {
        notNullOrBlank(alias, "alias");
        notNullOrBlank(index, "index");
        return LettuceCommand.discarding(() -> async.ftAliasupdate(alias, index));
    }

    @Override
    public Uni<Void> ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        return _ftAlter(index, field, skipInitialScan).toUni();
    }

    LettuceCommand<String, Void> _ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        notNullOrBlank(index, "index");
        nonNull(field, "field");
        FieldArgs fieldArgs = LettuceSearchCommandsConverters.toLettuceFieldArgs(field);
        return LettuceCommand.discarding(() -> async.ftAlter(index, skipInitialScan, List.of(fieldArgs)));
    }

    @Override
    public Uni<Void> ftCreate(String index, CreateArgs args) {
        return _ftCreate(index, args).toUni();
    }

    LettuceCommand<String, Void> _ftCreate(String index, CreateArgs args) {
        notNullOrBlank(index, "index");
        nonNull(args, "args");
        if (args.getSchema().isEmpty()) {
            throw new IllegalArgumentException("The schema cannot be empty, you configure the indexed fields");
        }
        io.lettuce.core.search.arguments.CreateArgs createArgs = LettuceSearchCommandsConverters.toLettuceCreateArgs(args);
        List<FieldArgs> schema = args.getSchema().stream().map(LettuceSearchCommandsConverters::toLettuceFieldArgs).toList();
        return LettuceCommand.discarding(() -> async.ftCreate(index, createArgs, schema));
    }

    @Override
    public Uni<Void> ftCursorDel(String index, long cursor) {
        return _ftCursorDel(index, cursor).toUni();
    }

    LettuceCommand<String, Void> _ftCursorDel(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        AggregationReply.Cursor lettuceCursor = cursors.cursor(index, cursor);
        return LettuceCommand.<String, Void> of(() -> async.ftCursordel(index, lettuceCursor), ignored -> {
            cursors.forget(index, cursor);
            return null;
        }).onFailure(failure -> cursors.forgetIfGone(index, cursor, failure));
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor) {
        return _ftCursorRead(index, cursor).toUni();
    }

    LettuceCommand<AggregationReply<byte[]>, AggregationResponse> _ftCursorRead(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        AggregationReply.Cursor lettuceCursor = cursors.cursor(index, cursor);
        return LettuceCommand.of(() -> async.ftCursorread(index, lettuceCursor),
                reply -> decodeAggregationReply(index, cursor, reply))
                .onFailure(failure -> cursors.forgetIfGone(index, cursor, failure));
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor, int count) {
        return _ftCursorRead(index, cursor, count).toUni();
    }

    LettuceCommand<AggregationReply<byte[]>, AggregationResponse> _ftCursorRead(String index, long cursor, int count) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        positive(count, "count");
        AggregationReply.Cursor lettuceCursor = cursors.cursor(index, cursor);
        return LettuceCommand.of(() -> async.ftCursorread(index, lettuceCursor, count),
                reply -> decodeAggregationReply(index, cursor, reply))
                .onFailure(failure -> cursors.forgetIfGone(index, cursor, failure));
    }

    @Override
    public Uni<Void> ftDropIndex(String index) {
        return _ftDropIndex(index).toUni();
    }

    LettuceCommand<String, Void> _ftDropIndex(String index) {
        notNullOrBlank(index, "index");
        return LettuceCommand.of(() -> async.ftDropindex(index), ignored -> {
            cursors.forgetIndex(index);
            return null;
        });
    }

    @Override
    public Uni<Void> ftDropIndex(String index, boolean dd) {
        return _ftDropIndex(index, dd).toUni();
    }

    LettuceCommand<String, Void> _ftDropIndex(String index, boolean dd) {
        notNullOrBlank(index, "index");
        return LettuceCommand.of(() -> async.ftDropindex(index, dd), ignored -> {
            cursors.forgetIndex(index);
            return null;
        });
    }

    @Override
    public Uni<Void> ftDictAdd(String dict, String... words) {
        return _ftDictAdd(dict, words).toUni();
    }

    LettuceCommand<Long, Void> _ftDictAdd(String dict, String... words) {
        notNullOrBlank(dict, "dict");
        notNullOrEmpty(words, "words");
        doesNotContainNull(words, "words");
        return LettuceCommand.discarding(() -> async.ftDictadd(dict, words));
    }

    @Override
    public Uni<Void> ftDictDel(String dict, String... words) {
        return _ftDictDel(dict, words).toUni();
    }

    LettuceCommand<Long, Void> _ftDictDel(String dict, String... words) {
        notNullOrBlank(dict, "dict");
        notNullOrEmpty(words, "words");
        doesNotContainNull(words, "words");
        return LettuceCommand.discarding(() -> async.ftDictdel(dict, words));
    }

    @Override
    public Uni<List<String>> ftDictDump(String dict) {
        return _ftDictDump(dict).toUni();
    }

    LettuceCommand<List<String>, List<String>> _ftDictDump(String dict) {
        notNullOrBlank(dict, "dict");
        return LettuceCommand.of(() -> async.ftDictdump(dict));
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query, QueryArgs args) {
        return _ftSearch(index, query, args).toUni();
    }

    LettuceCommand<SearchReply<byte[]>, SearchQueryResponse> _ftSearch(String index, String query, QueryArgs args) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        SearchArgs<byte[]> searchArgs = LettuceSearchCommandsConverters.toLettuceSearchArgs(args);
        return LettuceCommand.of(() -> async.ftSearch(index, query, searchArgs), this::decodeSearchReply);
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query) {
        return _ftSearch(index, query).toUni();
    }

    LettuceCommand<SearchReply<byte[]>, SearchQueryResponse> _ftSearch(String index, String query) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        return LettuceCommand.of(() -> async.ftSearch(index, query), this::decodeSearchReply);
    }

    @Override
    public Uni<SpellCheckResponse> ftSpellCheck(String index, String query) {
        return _ftSpellCheck(index, query).toUni();
    }

    LettuceCommand<SpellCheckResult, SpellCheckResponse> _ftSpellCheck(String index, String query) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        return LettuceCommand.of(() -> async.ftSpellcheck(index, query),
                LettuceReactiveSearchCommandsImpl::decodeSpellCheckResult);
    }

    @Override
    public Uni<SpellCheckResponse> ftSpellCheck(String index, String query, SpellCheckArgs args) {
        return _ftSpellCheck(index, query, args).toUni();
    }

    LettuceCommand<SpellCheckResult, SpellCheckResponse> _ftSpellCheck(String index, String query, SpellCheckArgs args) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        io.lettuce.core.search.arguments.SpellCheckArgs lettuceArgs = LettuceSearchCommandsConverters
                .toLettuceSpellCheckArgs(args);
        return LettuceCommand.of(() -> async.ftSpellcheck(index, query, lettuceArgs),
                LettuceReactiveSearchCommandsImpl::decodeSpellCheckResult);
    }

    @Override
    public Uni<SynDumpResponse> ftSynDump(String index) {
        return _ftSynDump(index).toUni();
    }

    LettuceCommand<Map<String, List<String>>, SynDumpResponse> _ftSynDump(String index) {
        notNullOrBlank(index, "index");
        return LettuceCommand.of(() -> async.ftSyndump(index), LettuceReactiveSearchCommandsImpl::decodeSynDump);
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, String... words) {
        return _ftSynUpdate(index, groupId, words).toUni();
    }

    LettuceCommand<String, Void> _ftSynUpdate(String index, String groupId, String... words) {
        notNullOrBlank(index, "index");
        notNullOrBlank(groupId, "groupId");
        doesNotContainNull(words, "words");
        notNullOrEmpty(words, "words");
        return LettuceCommand.discarding(() -> async.ftSynupdate(index, groupId, words));
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        return _ftSynUpdate(index, groupId, skipInitialScan, words).toUni();
    }

    LettuceCommand<String, Void> _ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        notNullOrBlank(index, "index");
        notNullOrBlank(groupId, "groupId");
        doesNotContainNull(words, "words");
        notNullOrEmpty(words, "words");
        if (skipInitialScan) {
            return LettuceCommand
                    .discarding(() -> async.ftSynupdate(index, groupId, SynUpdateArgs.Builder.skipInitialScan(), words));
        }
        return LettuceCommand.discarding(() -> async.ftSynupdate(index, groupId, words));
    }

    @Override
    public Uni<Set<String>> ftTagVals(String index, String field) {
        return _ftTagVals(index, field).toUni();
    }

    LettuceCommand<List<String>, Set<String>> _ftTagVals(String index, String field) {
        notNullOrBlank(index, "index");
        notNullOrBlank(field, "field");
        return LettuceCommand.of(() -> async.ftTagvals(index, field), Set::copyOf);
    }

    private List<K> decodeIndexNames(List<String> names) {
        return orEmpty(names).stream()
                .map(name -> decodeK(name.getBytes(StandardCharsets.UTF_8)))
                .toList();
    }

    private AggregationResponse decodeAggregationReply(String index, long requestedCursor,
            AggregationReply<byte[]> reply) {
        return decodeAggregationReply(index, requestedCursor, null, reply);
    }

    private AggregationResponse decodeAggregationReply(String index, long requestedCursor, Duration cursorMaxIdle,
            AggregationReply<byte[]> reply) {
        cursors.track(index, requestedCursor, reply, cursorMaxIdle);
        return decodeAggregationReply(reply);
    }

    private AggregationResponse decodeAggregationReply(AggregationReply<byte[]> reply) {
        if (reply == null) {
            return new AggregationResponse(Collections.emptyList());
        }
        long cursor = reply.getCursor().map(AggregationReply.Cursor::getCursorId).orElse(-1L);
        List<AggregateDocument> documents = new ArrayList<>();
        for (SearchReply<byte[]> searchReply : orEmpty(reply.getReplies())) {
            for (SearchReply.SearchResult<byte[]> result : orEmpty(searchReply.getResults())) {
                documents.add(new AggregateDocument(toProperties(result.getFields())));
            }
        }
        return new AggregationResponse(cursor, documents);
    }

    private SearchQueryResponse decodeSearchReply(SearchReply<byte[]> reply) {
        if (reply == null) {
            return new SearchQueryResponse(0, Collections.emptyList());
        }
        List<Document> documents = new ArrayList<>(reply.size());
        for (SearchReply.SearchResult<byte[]> result : orEmpty(reply.getResults())) {
            String id = result.getId() == null ? null : decodeString(result.getId());
            // The score is only present with WITHSCORES, the Vert.x backend also reports 1.0 otherwise
            double score = result.getScore() == null ? 1.0 : result.getScore();
            // Lettuce does not support WITHPAYLOADS (rejected by the converter), so there is never a payload
            documents.add(new Document(id, score, null, toProperties(result.getFields())));
        }
        return new SearchQueryResponse((int) reply.getCount(), documents);
    }

    private static Map<String, Document.Property> toProperties(Map<String, FieldValue> fields) {
        Map<String, Document.Property> properties = new LinkedHashMap<>();
        if (fields == null) {
            return properties;
        }
        for (Map.Entry<String, FieldValue> field : fields.entrySet()) {
            properties.put(field.getKey(), new Document.Property(field.getKey(), toVertxResponse(field.getValue())));
        }
        return properties;
    }

    private static Response toVertxResponse(FieldValue value) {
        if (value == null || value.isNull()) {
            return null;
        }
        switch (value.getKind()) {
            case ARRAY: {
                List<FieldValue> items = value.asList();
                MultiType multi = MultiType.create(items.size(), false);
                for (FieldValue item : items) {
                    multi.add(toVertxResponse(item));
                }
                return multi;
            }
            case MAP: {
                Map<String, FieldValue> entries = value.asMap();
                MultiType multi = MultiType.create(entries.size(), true);
                for (Map.Entry<String, FieldValue> entry : entries.entrySet()) {
                    multi.add(BulkType.create(Buffer.buffer(entry.getKey()), false));
                    multi.add(toVertxResponse(entry.getValue()));
                }
                return multi;
            }
            default:
                return BulkType.create(Buffer.buffer(value.asBytes()), false);
        }
    }

    private static SynDumpResponse decodeSynDump(Map<String, List<String>> termsToGroups) {
        Map<String, List<String>> groupsToTerms = new LinkedHashMap<>();
        if (termsToGroups != null) {
            for (Map.Entry<String, List<String>> entry : termsToGroups.entrySet()) {
                for (String group : orEmpty(entry.getValue())) {
                    groupsToTerms.computeIfAbsent(group, ignored -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }
        return new SynDumpResponse(groupsToTerms);
    }

    private static SpellCheckResponse decodeSpellCheckResult(SpellCheckResult result) {
        if (result == null || !result.hasMisspelledTerms()) {
            return new SpellCheckResponse(Collections.emptyMap());
        }
        Map<String, List<SpellCheckResponse.SpellCheckSuggestion>> response = new LinkedHashMap<>();
        for (SpellCheckResult.MisspelledTerm term : result.getMisspelledTerms()) {
            String word = term.getTerm();
            if (!term.hasSuggestions()) {
                response.put(word, Collections.emptyList());
                continue;
            }
            List<SpellCheckResponse.SpellCheckSuggestion> suggestions = new ArrayList<>();
            for (SpellCheckResult.Suggestion suggestion : term.getSuggestions()) {
                // Same as the Vert.x backend: a proposal identical to the term is not a correction
                if (!word.equals(suggestion.getSuggestion())) {
                    suggestions.add(new SpellCheckResponse.SpellCheckSuggestion(suggestion.getSuggestion(),
                            suggestion.getScore()));
                }
            }
            if (!suggestions.isEmpty()) {
                response.put(word, suggestions);
            }
        }
        return new SpellCheckResponse(response);
    }

}
