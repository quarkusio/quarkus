package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.autosuggest.AutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.Suggestion;

public class BlockingAutoSuggestCommandsImpl<K> extends AbstractRedisCommandGroup implements AutoSuggestCommands<K> {

    private final ReactiveAutoSuggestCommands<K> reactive;

    public BlockingAutoSuggestCommandsImpl(RedisDataSource ds, ReactiveAutoSuggestCommands<K> reactive,
            Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public long ftSugAdd(K key, String string, double score, boolean increment) {
        return reactive.ftSugAdd(key, string, score, increment).await().atMost(timeout);
    }

    @Override
    public boolean ftSugDel(K key, String string) {
        return reactive.ftSugDel(key, string).await().atMost(timeout);
    }

    @Override
    public List<Suggestion> ftSugGet(K key, String prefix) {
        return reactive.ftSugGet(key, prefix).await().atMost(timeout);
    }

    @Override
    public List<Suggestion> ftSugGet(K key, String prefix, GetArgs args) {
        return reactive.ftSugGet(key, prefix, args).await().atMost(timeout);
    }

    @Override
    public long ftSugLen(K key) {
        return reactive.ftSugLen(key).await().atMost(timeout);
    }
}
