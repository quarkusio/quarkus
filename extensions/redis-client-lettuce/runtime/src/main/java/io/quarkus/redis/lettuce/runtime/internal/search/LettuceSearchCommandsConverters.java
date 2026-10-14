package io.quarkus.redis.lettuce.runtime.internal.search;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.lettuce.core.search.arguments.AggregateArgs.Apply;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.arguments.AggregateArgs.SortBy;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;
import io.lettuce.core.search.arguments.AggregateArgs.SortProperty;
import io.lettuce.core.search.arguments.AggregateArgs.WithCursor;
import io.lettuce.core.search.arguments.CreateArgs.TargetType;
import io.lettuce.core.search.arguments.DocumentLanguage;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.GeoFieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.QueryDialects;
import io.lettuce.core.search.arguments.ScoringFunction;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs.PhoneticMatcher;
import io.lettuce.core.search.arguments.VectorFieldArgs;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.FieldOptions;
import io.quarkus.redis.datasource.search.FieldType;
import io.quarkus.redis.datasource.search.HighlightArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.search.SummarizeArgs;

final class LettuceSearchCommandsConverters {

    private LettuceSearchCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.search.arguments.AggregateArgs toLettuceAggregateArgs(AggregateArgs quarkus) {
        io.lettuce.core.search.arguments.AggregateArgs.Builder builder = getBuilder(quarkus);
        if (quarkus.getTimeout() != null) {
            builder.timeout(quarkus.getTimeout());
        }
        for (Map.Entry<String, Object> param : quarkus.getParams().entrySet()) {
            builder.param(param.getKey(), param.getValue().toString());
        }
        for (AggregateArgs.Clause clause : quarkus.getClauses()) {
            if (clause instanceof AggregateArgs.GroupBy groupBy) {
                builder.groupBy(toLettuceGroupBy(groupBy));
            } else if (clause instanceof AggregateArgs.SortBy sortBy) {
                builder.sortBy(toLettuceSortBy(sortBy));
            } else if (clause instanceof AggregateArgs.Apply apply) {
                builder.apply(Apply.of(apply.getExpression(), apply.getAlias()));
            } else if (clause instanceof AggregateArgs.Filter filter) {
                builder.filter(filter.getFilter());
            } else {
                throw new IllegalArgumentException("Unsupported aggregation clause: " + clause.getClass().getName());
            }
        }
        if (quarkus.getOffset() != -1) {
            builder.limit(quarkus.getOffset(), quarkus.getCount());
        }
        if (quarkus.getDialect() != -1) {
            builder.dialect(toLettuceDialect(quarkus.getDialect()));
        }
        if (quarkus.hasCursor()) {
            builder.withCursor(toLettuceWithCursor(quarkus));
        }
        return builder.build();
    }

    private static io.lettuce.core.search.arguments.AggregateArgs.Builder getBuilder(AggregateArgs quarkus) {
        io.lettuce.core.search.arguments.AggregateArgs.Builder builder = io.lettuce.core.search.arguments.AggregateArgs
                .builder();
        if (quarkus.isVerbatim()) {
            builder.verbatim();
        }
        if (quarkus.isLoadAllFields()) {
            if (!quarkus.getFields().isEmpty()) {
                throw new IllegalArgumentException(
                        "Lettuce cannot combine `LOAD *` with named fields, use either allFields() or field(...)");
            }
            builder.loadAll();
        }
        for (Map.Entry<String, String> field : quarkus.getFields().entrySet()) {
            if (field.getValue() == null) {
                builder.load(field.getKey());
            } else {
                builder.load(field.getKey(), field.getValue());
            }
        }
        return builder;
    }

    private static GroupBy toLettuceGroupBy(AggregateArgs.GroupBy quarkus) {
        GroupBy groupBy = new GroupBy(quarkus.getProperties());
        for (AggregateArgs.ReduceFunction function : quarkus.getReduceFunctions()) {
            List<String> args = function.getArgs().stream().map(Object::toString).toList();
            Reducer reducer = new Reducer(function.getFunction(), args);
            if (function.getAlias() != null) {
                reducer.as(function.getAlias());
            }
            groupBy.reduce(reducer);
        }
        return groupBy;
    }

    private static SortBy toLettuceSortBy(AggregateArgs.SortBy quarkus) {
        List<SortProperty> properties = quarkus.getProperties().entrySet().stream()
                .map(e -> new SortProperty(e.getKey(), SortDirection.valueOf(e.getValue())))
                .toList();
        SortBy sortBy = new SortBy(properties);
        if (quarkus.getMax() != -1) {
            sortBy.max(quarkus.getMax());
        }
        return sortBy;
    }

    private static QueryDialects toLettuceDialect(int dialect) {
        return switch (dialect) {
            case 1 -> QueryDialects.DIALECT1;
            case 2 -> QueryDialects.DIALECT2;
            case 3 -> QueryDialects.DIALECT3;
            case 4 -> QueryDialects.DIALECT4;
            default -> throw new IllegalArgumentException("Unsupported query dialect: " + dialect);
        };
    }

    private static WithCursor toLettuceWithCursor(AggregateArgs quarkus) {
        Long count = quarkus.getCursorCount() == -1 ? null : (long) quarkus.getCursorCount();
        Duration maxIdle = quarkus.getCursorMaxIdle();
        return maxIdle == null ? WithCursor.of(count) : WithCursor.of(count, maxIdle);
    }

    static FieldArgs toLettuceFieldArgs(IndexedField quarkus) {
        FieldOptions options = quarkus.getOptions() == null ? new FieldOptions() : quarkus.getOptions();
        FieldType type = quarkus.getType();
        requireType(options.isNoStem(), "noStem", type, FieldType.TEXT);
        requireType(options.getPhonetic() != null, "phonetic", type, FieldType.TEXT);
        requireType(options.getWeight() != -1, "weight", type, FieldType.TEXT);
        requireType(options.getSeparator() != 0, "separator", type, FieldType.TAG);
        requireType(options.isCaseSensitive(), "caseSensitive", type, FieldType.TAG);
        requireType(options.isWithSuffixTrie(), "withSuffixTrie", type, FieldType.TEXT, FieldType.TAG);
        requireType(options.getVectorAlgorithm() != null, "vectorAlgorithm", type, FieldType.VECTOR);
        requireType(options.getVectorType() != null, "vectorType", type, FieldType.VECTOR);
        requireType(options.getDimension() != null, "dimension", type, FieldType.VECTOR);
        requireType(options.getDistanceMetric() != null, "distanceMetric", type, FieldType.VECTOR);
        requireType(options.getInitialCap() != null, "initialCap", type, FieldType.VECTOR);
        requireType(options.getBlockSize() != null, "blockSize", type, FieldType.VECTOR);

        FieldArgs.Builder<?, ?> builder = switch (type) {
            case TEXT -> toLettuceTextField(options);
            case TAG -> toLettuceTagField(options);
            case NUMERIC -> NumericFieldArgs.builder();
            case GEO -> GeoFieldArgs.builder();
            case VECTOR -> toLettuceVectorField(options);
        };
        builder.name(quarkus.getField());
        if (quarkus.getAlias() != null) {
            builder.as(quarkus.getAlias());
        }
        if (options.isSortable()) {
            builder.sortable();
        }
        if (options.isUnf()) {
            builder.unNormalizedForm();
        }
        if (options.isNoIndex()) {
            builder.noIndex();
        }
        return builder.build();
    }

    private static TextFieldArgs.Builder toLettuceTextField(FieldOptions options) {
        TextFieldArgs.Builder builder = TextFieldArgs.builder();
        if (options.getWeight() != -1) {
            builder.weight(toLettuceWeight(options.getWeight()));
        }
        if (options.isNoStem()) {
            builder.noStem();
        }
        if (options.getPhonetic() != null) {
            builder.phonetic(toLettucePhoneticMatcher(options.getPhonetic()));
        }
        if (options.isWithSuffixTrie()) {
            builder.withSuffixTrie();
        }
        return builder;
    }

    private static TagFieldArgs.Builder toLettuceTagField(FieldOptions options) {
        TagFieldArgs.Builder builder = TagFieldArgs.builder();
        if (options.getSeparator() != 0) {
            builder.separator(Character.toString(options.getSeparator()));
        }
        if (options.isCaseSensitive()) {
            builder.caseSensitive();
        }
        if (options.isWithSuffixTrie()) {
            builder.withSuffixTrie();
        }
        return builder;
    }

    private static VectorFieldArgs.Builder toLettuceVectorField(FieldOptions options) {
        VectorFieldArgs.Builder builder = VectorFieldArgs.builder();
        if (options.getVectorAlgorithm() != null) {
            builder.algorithm(VectorFieldArgs.Algorithm.valueOf(options.getVectorAlgorithm().name()));
        }
        if (options.getVectorType() != null) {
            builder.type(VectorFieldArgs.VectorType.valueOf(options.getVectorType().name()));
        }
        if (options.getDimension() != null) {
            builder.dimensions(options.getDimension());
        }
        if (options.getDistanceMetric() != null) {
            builder.distanceMetric(VectorFieldArgs.DistanceMetric.valueOf(options.getDistanceMetric().name()));
        }
        if (options.getInitialCap() != null) {
            builder.attribute("INITIAL_CAP", options.getInitialCap());
        }
        if (options.getBlockSize() != null) {
            builder.attribute("BLOCK_SIZE", options.getBlockSize());
        }
        return builder;
    }

    private static long toLettuceWeight(double weight) {
        if (weight != Math.rint(weight)) {
            throw new IllegalArgumentException("Lettuce only supports whole-number field weights, got " + weight);
        }
        return (long) weight;
    }

    private static PhoneticMatcher toLettucePhoneticMatcher(String phonetic) {
        for (PhoneticMatcher matcher : PhoneticMatcher.values()) {
            if (matcher.getMatcher().equalsIgnoreCase(phonetic)) {
                return matcher;
            }
        }
        throw new IllegalArgumentException("Unsupported phonetic matcher: " + phonetic);
    }

    private static void requireType(boolean set, String option, FieldType actual, FieldType... allowed) {
        if (set && !List.of(allowed).contains(actual)) {
            throw new IllegalArgumentException(
                    "`" + option + "` is only valid for " + List.of(allowed) + " fields, not " + actual);
        }
    }

    static io.lettuce.core.search.arguments.CreateArgs toLettuceCreateArgs(CreateArgs quarkus) {
        io.lettuce.core.search.arguments.CreateArgs.Builder builder = io.lettuce.core.search.arguments.CreateArgs
                .builder();
        if (quarkus.isOnHash() && quarkus.isOnJson()) {
            throw new IllegalArgumentException("Cannot use `ON HASH` and `ON JSON` at the same time");
        }
        if (quarkus.isOnHash()) {
            builder.on(TargetType.HASH);
        }
        if (quarkus.isOnJson()) {
            builder.on(TargetType.JSON);
        }
        if (!quarkus.getPrefixes().isEmpty()) {
            builder.withPrefixes(quarkus.getPrefixes());
        }
        if (quarkus.getFilter() != null) {
            builder.filter(quarkus.getFilter());
        }
        if (quarkus.getLanguage() != null) {
            builder.defaultLanguage(toLettuceLanguage(quarkus.getLanguage()));
        }
        if (quarkus.getLanguageField() != null) {
            builder.languageField(quarkus.getLanguageField());
        }
        if (quarkus.getScore() > 0) {
            builder.defaultScore(quarkus.getScore());
        }
        if (quarkus.getScoreField() != null) {
            builder.scoreField(quarkus.getScoreField());
        }
        if (quarkus.getPayloadField() != null) {
            builder.payloadField(quarkus.getPayloadField());
        }
        if (quarkus.isMaxTextFields()) {
            builder.maxTextFields();
        }
        if (quarkus.getTemporary() != null) {
            builder.temporary(quarkus.getTemporary().toSeconds());
        }
        if (quarkus.isNoOffsets()) {
            builder.noOffsets();
        }
        if (quarkus.isNoHl()) {
            builder.noHighlighting();
        }
        if (quarkus.isNoFields()) {
            builder.noFields();
        }
        if (quarkus.isNoFreqs()) {
            builder.noFrequency();
        }
        if (!quarkus.getStopWords().isEmpty()) {
            builder.stopWords(quarkus.getStopWords());
        }
        if (quarkus.isSkipInitialScan()) {
            builder.skipInitialScan();
        }
        return builder.build();
    }

    static SearchArgs<byte[]> toLettuceSearchArgs(QueryArgs quarkus) {
        if (quarkus.containsPayload()) {
            throw new IllegalArgumentException("Lettuce does not support `WITHPAYLOADS`");
        }
        if (!quarkus.getFilters().isEmpty() || !quarkus.getGeoFilters().isEmpty()) {
            throw new IllegalArgumentException("Lettuce does not support `FILTER` and `GEOFILTER`, "
                    + "express the range in the query string instead (`@price:[10 20]`, `@location:[lon lat 10 km]`)");
        }
        if (quarkus.isExplainScore()) {
            throw new IllegalArgumentException("Lettuce does not support `EXPLAINSCORE`");
        }
        String ascending = quarkus.getSortByAscending();
        String descending = quarkus.getSortByDescending();
        if (ascending != null && descending != null) {
            throw new IllegalArgumentException("Cannot use descending and ascending order at the same time");
        }

        SearchArgs.Builder<byte[]> builder = SearchArgs.builder();
        if (quarkus.isNoContent()) {
            builder.noContent();
        }
        if (quarkus.isVerbatim()) {
            builder.verbatim();
        }
        if (quarkus.containsScore()) {
            builder.withScores();
        }
        if (quarkus.containsSortKeys()) {
            builder.withSortKeys();
        }
        for (String key : quarkus.getInKeys()) {
            builder.inKey(key.getBytes(StandardCharsets.UTF_8));
        }
        for (String field : quarkus.getInFields()) {
            builder.inField(field);
        }
        for (QueryArgs.ReturnClause clause : quarkus.getReturnClauses()) {
            if (clause.getAlias() == null) {
                builder.returnField(clause.getField());
            } else {
                builder.returnField(clause.getField(), clause.getAlias());
            }
        }
        if (quarkus.getSummarize() != null) {
            builder.summarizeArgs(toLettuceSummarizeArgs(quarkus.getSummarize()));
        }
        if (quarkus.getHighlight() != null) {
            builder.highlightArgs(toLettuceHighlightArgs(quarkus.getHighlight()));
        }
        if (quarkus.getSlop() != -1) {
            builder.slop(quarkus.getSlop());
        }
        if (quarkus.isInOrder()) {
            builder.inOrder();
        }
        if (quarkus.getLanguage() != null) {
            builder.language(toLettuceLanguage(quarkus.getLanguage()));
        }
        if (quarkus.getExpander() != null) {
            builder.expander(quarkus.getExpander());
        }
        if (quarkus.getScorer() != null) {
            builder.scorer(toLettuceScoringFunction(quarkus.getScorer()));
        }
        if (ascending != null) {
            builder.sortBy(SortByArgs.builder().attribute(ascending).build());
        }
        if (descending != null) {
            builder.sortBy(SortByArgs.builder().attribute(descending).descending().build());
        }
        if (quarkus.getOffset() != -1) {
            builder.limit(quarkus.getOffset(), quarkus.getCount());
        }
        if (quarkus.getTimeout() != null) {
            builder.timeout(quarkus.getTimeout());
        }
        for (Map.Entry<String, byte[]> param : quarkus.getByteArrayParams().entrySet()) {
            builder.param(param.getKey(), param.getValue());
        }
        for (Map.Entry<String, String> param : quarkus.getParams().entrySet()) {
            builder.param(param.getKey(), param.getValue());
        }
        // Lettuce always sends DIALECT and defaults to 2 (the server default since RediSearch 2.10 / Redis 8),
        // so an unset Quarkus dialect cannot fall back to the server setting the way the Vert.x backend does.
        if (quarkus.getDialect() != -1) {
            builder.dialect(toLettuceDialect(quarkus.getDialect()));
        }
        return builder.build();
    }

    static io.lettuce.core.search.arguments.SpellCheckArgs toLettuceSpellCheckArgs(SpellCheckArgs quarkus) {
        List<String> includes = quarkus.getIncludes();
        List<String> excludes = quarkus.getExcludes();
        if (!includes.isEmpty() && !excludes.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify both `includes` and `excludes` terms");
        }
        io.lettuce.core.search.arguments.SpellCheckArgs lettuce = new io.lettuce.core.search.arguments.SpellCheckArgs();
        if (quarkus.getDistance() != 0) {
            lettuce.distance(quarkus.getDistance());
        }
        // `TERMS INCLUDE|EXCLUDE dict [term...]`: the first Quarkus value is the dictionary, the rest are terms
        if (!includes.isEmpty()) {
            lettuce.termsInclude(includes.get(0), includes.subList(1, includes.size()).toArray(String[]::new));
        }
        if (!excludes.isEmpty()) {
            lettuce.termsExclude(excludes.get(0), excludes.subList(1, excludes.size()).toArray(String[]::new));
        }
        if (quarkus.getDialect() != -1) {
            lettuce.dialect(quarkus.getDialect());
        }
        return lettuce;
    }

    private static io.lettuce.core.search.arguments.SummarizeArgs toLettuceSummarizeArgs(SummarizeArgs quarkus) {
        io.lettuce.core.search.arguments.SummarizeArgs.Builder builder = io.lettuce.core.search.arguments.SummarizeArgs
                .builder();
        for (String field : quarkus.getFields()) {
            builder.field(field);
        }
        if (quarkus.getFragments() > 0) {
            builder.fragments(quarkus.getFragments());
        }
        if (quarkus.getLength() > 0) {
            builder.len(quarkus.getLength());
        }
        if (quarkus.getSeparator() != null) {
            builder.separator(quarkus.getSeparator());
        }
        return builder.build();
    }

    private static io.lettuce.core.search.arguments.HighlightArgs toLettuceHighlightArgs(HighlightArgs quarkus) {
        io.lettuce.core.search.arguments.HighlightArgs.Builder builder = io.lettuce.core.search.arguments.HighlightArgs
                .builder();
        for (String field : quarkus.getFields()) {
            builder.field(field);
        }
        if (quarkus.getOpenTag() != null && quarkus.getCloseTag() != null) {
            builder.tags(quarkus.getOpenTag(), quarkus.getCloseTag());
        }
        return builder.build();
    }

    private static ScoringFunction toLettuceScoringFunction(String scorer) {
        for (ScoringFunction function : ScoringFunction.values()) {
            if (function.toString().equalsIgnoreCase(scorer)) {
                return function;
            }
        }
        throw new IllegalArgumentException("Unsupported scoring function: " + scorer);
    }

    private static DocumentLanguage toLettuceLanguage(String language) {
        for (DocumentLanguage candidate : DocumentLanguage.values()) {
            if (candidate.toString().equalsIgnoreCase(language)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported document language: " + language);
    }

}
