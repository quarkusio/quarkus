package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.Suggestion;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveAutoSuggestCommandsImpl<K> extends AbstractAutoSuggestCommands<K>
        implements ReactiveAutoSuggestCommands<K>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;

    public ReactiveAutoSuggestCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Long> ftSugAdd(K key, String string, double score, boolean increment) {
        return super._ftSugAdd(key, string, score, increment).map(Response::toLong);
    }

    @Override
    public Uni<Boolean> ftSugDel(K key, String string) {
        return super._ftSugDel(key, string).map(Response::toBoolean);
    }

    @Override
    public Uni<List<Suggestion>> ftSugGet(K key, String prefix) {
        return super._ftSugget(key, prefix).map(r -> decodeAsListOfSuggestion(r, false));
    }

    @Override
    public Uni<List<Suggestion>> ftSugGet(K key, String prefix, GetArgs args) {
        return super._ftSugget(key, prefix, args).map(r -> decodeAsListOfSuggestion(r, args.hasScores()));
    }

    List<Suggestion> decodeAsListOfSuggestion(Response response, boolean hasScores) {
        List<Suggestion> list = new ArrayList<>();
        if (hasScores) {
            String current = null;
            for (Response nested : response) {
                if (current == null) {
                    current = nested.toString();
                } else {
                    list.add(new Suggestion(current, nested.toDouble()));
                    current = null;
                }
            }
        } else {
            for (Response nested : response) {
                list.add(new Suggestion(nested.toString()));
            }
        }
        return list;
    }

    @Override
    public Uni<Long> ftSugLen(K key) {
        return super._ftSugLen(key).map(Response::toLong);
    }
}
