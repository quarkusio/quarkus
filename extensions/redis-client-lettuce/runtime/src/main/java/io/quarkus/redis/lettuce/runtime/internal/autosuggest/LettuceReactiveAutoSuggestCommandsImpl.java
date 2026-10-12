package io.quarkus.redis.lettuce.runtime.internal.autosuggest;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.search.arguments.SugAddArgs;
import io.lettuce.core.search.arguments.SugGetArgs;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveAutoSuggestCommands}.
 *
 * @param <K> the key type
 */
public class LettuceReactiveAutoSuggestCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveAutoSuggestCommands<K> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveAutoSuggestCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Long> ftSugAdd(K key, String string, double score, boolean increment) {
        return _ftSugAdd(key, string, score, increment).toUni();
    }

    LettuceCommand<Long, Long> _ftSugAdd(K key, String string, double score, boolean increment) {
        nonNull(key, "key");
        notNullOrBlank(string, "string");
        SugAddArgs args = new SugAddArgs();
        if (increment) {
            args.incr();
        }
        return LettuceCommand.of(() -> async.ftSugadd(marshaller.encode(key), string, score, args));
    }

    @Override
    public Uni<Boolean> ftSugDel(K key, String string) {
        return _ftSugDel(key, string).toUni();
    }

    LettuceCommand<Boolean, Boolean> _ftSugDel(K key, String string) {
        nonNull(key, "key");
        notNullOrBlank(string, "string");
        return LettuceCommand.of(() -> async.ftSugdel(marshaller.encode(key), string));
    }

    @Override
    public Uni<List<Suggestion>> ftSugGet(K key, String prefix) {
        return _ftSugGet(key, prefix).toUni();
    }

    LettuceCommand<List<io.lettuce.core.search.Suggestion>, List<Suggestion>> _ftSugGet(K key, String prefix) {
        nonNull(key, "key");
        notNullOrBlank(prefix, "prefix");
        return LettuceCommand.of(() -> async.ftSugget(marshaller.encode(key), prefix), this::decodeSuggestionList);
    }

    @Override
    public Uni<List<Suggestion>> ftSugGet(K key, String prefix, GetArgs args) {
        return _ftSugGet(key, prefix, args).toUni();
    }

    LettuceCommand<List<io.lettuce.core.search.Suggestion>, List<Suggestion>> _ftSugGet(K key, String prefix, GetArgs args) {
        nonNull(key, "key");
        notNullOrBlank(prefix, "prefix");
        nonNull(args, "args");
        SugGetArgs lettuceArgs = toLettuceSugGetArgs(args);
        return LettuceCommand.of(() -> async.ftSugget(marshaller.encode(key), prefix, lettuceArgs), this::decodeSuggestionList);
    }

    @Override
    public Uni<Long> ftSugLen(K key) {
        return _ftSugLen(key).toUni();
    }

    LettuceCommand<Long, Long> _ftSugLen(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.ftSuglen(marshaller.encode(key)));
    }

    private SugGetArgs toLettuceSugGetArgs(GetArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        SugGetArgs lettuceArgs = new SugGetArgs() {
            @Override
            public void build(CommandArgs<?, ?> args) {
                ArgReplay.replay(tokens, args);
            }
        };
        if (quarkus.hasScores()) {
            lettuceArgs.withScores();
        }
        return lettuceArgs;
    }

    private Suggestion decodeSuggestion(io.lettuce.core.search.Suggestion lettuce) {
        if (lettuce.getScore() == null) {
            return new Suggestion(lettuce.getValue());
        }
        return new Suggestion(lettuce.getValue(), lettuce.getScore());
    }

    private List<Suggestion> decodeSuggestionList(List<io.lettuce.core.search.Suggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        return suggestions.stream().map(this::decodeSuggestion).toList();
    }

}
