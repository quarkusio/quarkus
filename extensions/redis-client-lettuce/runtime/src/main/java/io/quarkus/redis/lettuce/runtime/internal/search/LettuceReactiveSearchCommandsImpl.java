package io.quarkus.redis.lettuce.runtime.internal.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ComplexOutput;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.SpellCheckResultParser;
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
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.VertxResponseParser;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import io.vertx.redis.client.impl.types.MultiType;

public class LettuceReactiveSearchCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveSearchCommands<K> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveSearchCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
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

    LettuceCommand<Response, AggregationResponse> _ftAggregate(String indexName, String query, AggregateArgs args) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(indexName, query);
        ArgReplay.replay(args.toArgs(), commandArgs);
        boolean cursor = args.hasCursor();
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_AGGREGATE, responseOutput(), commandArgs),
                reply -> decodeAggregateResponse(reply, cursor));
    }

    @Override
    public Uni<AggregationResponse> ftAggregate(String indexName, String query) {
        return _ftAggregate(indexName, query).toUni();
    }

    LettuceCommand<Response, AggregationResponse> _ftAggregate(String indexName, String query) {
        notNullOrBlank(indexName, "indexName");
        notNullOrBlank(query, "query");
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(indexName, query);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_AGGREGATE, responseOutput(), commandArgs),
                reply -> decodeAggregateResponse(reply, false));
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
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(index);
        if (skipInitialScan) {
            commandArgs.add(CommandKeyword.SKIPINITIALSCAN);
        }
        commandArgs.add(CommandKeyword.SCHEMA).add(CommandKeyword.ADD);
        ArgReplay.replay(field.toArgs(), commandArgs);
        return LettuceCommand.discarding(
                () -> async.dispatch(CommandType.FT_ALTER, new StatusOutput<>(ByteArrayCodec.INSTANCE), commandArgs));
    }

    @Override
    public Uni<Void> ftCreate(String index, CreateArgs args) {
        return _ftCreate(index, args).toUni();
    }

    LettuceCommand<String, Void> _ftCreate(String index, CreateArgs args) {
        notNullOrBlank(index, "index");
        nonNull(args, "args");
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(index);
        // toArgs() rejects an empty schema and `ON HASH` combined with `ON JSON`, before the command is issued
        ArgReplay.replay(args.toArgs(), commandArgs);
        return LettuceCommand.discarding(
                () -> async.dispatch(CommandType.FT_CREATE, new StatusOutput<>(ByteArrayCodec.INSTANCE), commandArgs));
    }

    @Override
    public Uni<Void> ftCursorDel(String index, long cursor) {
        return _ftCursorDel(index, cursor).toUni();
    }

    LettuceCommand<String, Void> _ftCursorDel(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE).add(CommandKeyword.DEL).add(index)
                .add(cursor);
        return LettuceCommand.discarding(
                () -> async.dispatch(CommandType.FT_CURSOR, new StatusOutput<>(ByteArrayCodec.INSTANCE), commandArgs));
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor) {
        return _ftCursorRead(index, cursor).toUni();
    }

    LettuceCommand<Response, AggregationResponse> _ftCursorRead(String index, long cursor) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE).add(CommandKeyword.READ).add(index)
                .add(cursor);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_CURSOR, responseOutput(), commandArgs),
                reply -> decodeAggregateResponse(reply, true));
    }

    @Override
    public Uni<AggregationResponse> ftCursorRead(String index, long cursor, int count) {
        return _ftCursorRead(index, cursor, count).toUni();
    }

    LettuceCommand<Response, AggregationResponse> _ftCursorRead(String index, long cursor, int count) {
        notNullOrBlank(index, "index");
        positiveOrZero(cursor, "cursor");
        positive(count, "count");
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE).add(CommandKeyword.READ).add(index)
                .add(cursor)
                .add(CommandKeyword.COUNT).add(count);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_CURSOR, responseOutput(), commandArgs),
                reply -> decodeAggregateResponse(reply, true));
    }

    @Override
    public Uni<Void> ftDropIndex(String index) {
        return _ftDropIndex(index).toUni();
    }

    LettuceCommand<String, Void> _ftDropIndex(String index) {
        notNullOrBlank(index, "index");
        return LettuceCommand.discarding(() -> async.ftDropindex(index));
    }

    @Override
    public Uni<Void> ftDropIndex(String index, boolean dd) {
        return _ftDropIndex(index, dd).toUni();
    }

    LettuceCommand<String, Void> _ftDropIndex(String index, boolean dd) {
        notNullOrBlank(index, "index");
        return LettuceCommand.discarding(() -> async.ftDropindex(index, dd));
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
        return LettuceCommand.of(() -> async.ftDictdump(dict), AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query, QueryArgs args) {
        return _ftSearch(index, query, args).toUni();
    }

    LettuceCommand<Response, SearchQueryResponse> _ftSearch(String index, String query, QueryArgs args) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        nonNull(args, "args");
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(index, query);
        // toArgs() rejects an ascending and a descending SORTBY together, before the command is issued
        List<Object> tokens = args.toArgs();
        ArgReplay.replay(tokens, commandArgs);
        ResultLayout layout = ResultLayout.of(args, tokens);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_SEARCH, responseOutput(), commandArgs),
                reply -> decodeSearchResponse(reply, layout));
    }

    @Override
    public Uni<SearchQueryResponse> ftSearch(String index, String query) {
        return _ftSearch(index, query).toUni();
    }

    LettuceCommand<Response, SearchQueryResponse> _ftSearch(String index, String query) {
        notNullOrBlank(index, "index");
        notNullOrBlank(query, "query");
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(index, query);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_SEARCH, responseOutput(), commandArgs),
                reply -> decodeSearchResponse(reply, ResultLayout.DEFAULT));
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
        CommandArgs<byte[], byte[]> commandArgs = commandArgs(index, query);
        // toArgs() rejects `includes` combined with `excludes`, before the command is issued
        ArgReplay.replay(args.toArgs(), commandArgs);
        return LettuceCommand.of(() -> async.dispatch(CommandType.FT_SPELLCHECK,
                new EncodedComplexOutput<>(ByteArrayCodec.INSTANCE, new SpellCheckResultParser()), commandArgs),
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
        return LettuceCommand.of(() -> async.ftTagvals(index, field), AbstractLettuceCommands::decodeSet);
    }

    private static CommandArgs<byte[], byte[]> commandArgs(String... leading) {
        CommandArgs<byte[], byte[]> commandArgs = new CommandArgs<>(ByteArrayCodec.INSTANCE);
        for (String token : leading) {
            commandArgs.add(token);
        }
        return commandArgs;
    }

    private static ComplexOutput<byte[], byte[], Response> responseOutput() {
        return new ComplexOutput<>(ByteArrayCodec.INSTANCE, VertxResponseParser.INSTANCE);
    }

    private List<K> decodeIndexNames(List<String> names) {
        return decodeList(names, name -> decodeKey(name.getBytes(StandardCharsets.UTF_8)));
    }

    record ResultLayout(boolean withScores, boolean withPayloads, boolean withSortKeys, boolean withContent) {

        static final ResultLayout DEFAULT = new ResultLayout(false, false, false, true);

        static ResultLayout of(QueryArgs args, List<Object> tokens) {
            return new ResultLayout(args.containsScore(), args.containsPayload(), args.containsSortKeys(),
                    !tokens.contains("NOCONTENT"));
        }

        /** Number of reply elements per document in a RESP2 reply: the id plus one per enabled section. */
        int stride() {
            return 1 + (withScores ? 1 : 0) + (withPayloads ? 1 : 0) + (withSortKeys ? 1 : 0) + (withContent ? 1 : 0);
        }

    }

    static SearchQueryResponse decodeSearchResponse(Response response, ResultLayout layout) {
        if (response == null) {
            return new SearchQueryResponse(0, Collections.emptyList());
        }
        if (isMapReply(response)) {
            Response total = response.get("total_results");
            if (total == null || total.toInteger() == 0) {
                return new SearchQueryResponse(0, Collections.emptyList());
            }
            Response results = response.get("results");
            List<Document> documents = new ArrayList<>(sizeOf(results));
            for (Response result : elements(results)) {
                documents.add(new Document(result.get("id").toString(), score(result.get("score")), result.get("payload"),
                        properties(result.get("extra_attributes"))));
            }
            return new SearchQueryResponse(total.toInteger(), documents);
        }

        int count = response.get(0).toInteger();
        if (count == 0) {
            return new SearchQueryResponse(0, Collections.emptyList());
        }
        List<Document> documents = new ArrayList<>((response.size() - 1) / layout.stride());
        for (int i = 1; i < response.size();) {
            Response id = response.get(i++);
            Response score = layout.withScores() ? response.get(i++) : null;
            Response payload = layout.withPayloads() ? response.get(i++) : null;
            if (layout.withSortKeys()) {
                i++;
            }
            Response fields = layout.withContent() ? response.get(i++) : null;
            documents.add(new Document(id.toString(), score(score), payload, properties(fields)));
        }
        return new SearchQueryResponse(count, documents);
    }

    static AggregationResponse decodeAggregateResponse(Response response, boolean cursor) {
        if (response == null) {
            return new AggregationResponse(Collections.emptyList());
        }
        Response results = response;
        long cursorId = -1L;
        if (cursor) {
            results = response.get(0);
            cursorId = response.get(1).toLong();
        }
        if (isMapReply(results)) {
            Response rows = results.get("results");
            List<AggregateDocument> documents = new ArrayList<>(sizeOf(rows));
            for (Response result : elements(rows)) {
                documents.add(new AggregateDocument(properties(result.get("extra_attributes"))));
            }
            return new AggregationResponse(cursorId, documents);
        }

        // The first element is the group count, the rows follow
        List<AggregateDocument> documents = new ArrayList<>();
        for (int i = 1; i < results.size(); i++) {
            documents.add(new AggregateDocument(properties(results.get(i))));
        }
        return new AggregationResponse(cursorId, documents);
    }

    private static boolean isMap(Response response) {
        return response instanceof MultiType multi && multi.isMap();
    }

    /**
     * Whether a {@code FT.SEARCH} or {@code FT.AGGREGATE} reply is a RESP3 map, as such or flattened into an array of
     * alternating keys and values: Lettuce 8.0.0.BETA1 hands the RESP3 map reply of a command queued in
     * {@code MULTI}/{@code EXEC} to the command's output as a plain array (redis/lettuce#3937, fixed by
     * redis/lettuce#3959). The RESP2 replies start with the result count, so an even-sized array starting with a bulk
     * string is a flattened map, on which {@link MultiType} resolves the names pairwise.
     */
    private static boolean isMapReply(Response response) {
        if (!(response instanceof MultiType multi)) {
            return false;
        }
        if (multi.isMap()) {
            return true;
        }
        return multi.size() > 0 && multi.size() % 2 == 0 && multi.get(0) != null
                && multi.get(0).type() == ResponseType.BULK;
    }

    private static Iterable<Response> elements(Response multi) {
        return multi == null ? Collections.emptyList() : multi;
    }

    private static int sizeOf(Response multi) {
        return multi == null ? 0 : multi.size();
    }

    private static double score(Response score) {
        if (score == null) {
            return 1.0;
        }
        if (score.type() == ResponseType.MULTI) {
            return score.get(0).toDouble();
        }
        return score.toDouble();
    }

    private static Map<String, Document.Property> properties(Response fields) {
        if (fields == null) {
            return Collections.emptyMap();
        }
        if (isMap(fields)) {
            Map<String, Document.Property> properties = LinkedHashMap.newLinkedHashMap(fields.size());
            for (String name : fields.getKeys()) {
                properties.put(name, new Document.Property(name, fields.get(name)));
            }
            return properties;
        }
        Map<String, Document.Property> properties = LinkedHashMap.newLinkedHashMap(fields.size() / 2);
        String name = null;
        for (Response element : fields) {
            if (name == null) {
                name = element.toString();
            } else {
                properties.put(name, new Document.Property(name, element));
                name = null;
            }
        }
        return properties;
    }

    private static SynDumpResponse decodeSynDump(Map<String, List<String>> termsToGroups) {
        if (termsToGroups == null || termsToGroups.isEmpty()) {
            return new SynDumpResponse(Collections.emptyMap());
        }
        Map<String, List<String>> groupsToTerms = LinkedHashMap.newLinkedHashMap(termsToGroups.size());
        for (Map.Entry<String, List<String>> entry : termsToGroups.entrySet()) {
            for (String group : decodeList(entry.getValue())) {
                groupsToTerms.computeIfAbsent(group, ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }
        groupsToTerms.replaceAll((group, terms) -> Collections.unmodifiableList(terms));
        return new SynDumpResponse(Collections.unmodifiableMap(groupsToTerms));
    }

    private static SpellCheckResponse decodeSpellCheckResult(SpellCheckResult result) {
        if (result == null || !result.hasMisspelledTerms()) {
            return new SpellCheckResponse(Collections.emptyMap());
        }
        List<SpellCheckResult.MisspelledTerm> terms = result.getMisspelledTerms();
        Map<String, List<SpellCheckResponse.SpellCheckSuggestion>> response = LinkedHashMap.newLinkedHashMap(terms.size());
        for (SpellCheckResult.MisspelledTerm term : terms) {
            String word = term.getTerm();
            if (!term.hasSuggestions()) {
                response.put(word, Collections.emptyList());
                continue;
            }
            List<SpellCheckResponse.SpellCheckSuggestion> suggestions = new ArrayList<>(term.getSuggestions().size());
            for (SpellCheckResult.Suggestion suggestion : term.getSuggestions()) {
                // Same as the Vert.x backend: a proposal identical to the term is not a correction
                if (!word.equals(suggestion.getSuggestion())) {
                    suggestions.add(new SpellCheckResponse.SpellCheckSuggestion(suggestion.getSuggestion(),
                            suggestion.getScore()));
                }
            }
            if (!suggestions.isEmpty()) {
                response.put(word, Collections.unmodifiableList(suggestions));
            }
        }
        return new SpellCheckResponse(Collections.unmodifiableMap(response));
    }

}
