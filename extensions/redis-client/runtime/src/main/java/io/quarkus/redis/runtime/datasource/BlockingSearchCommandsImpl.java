package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.AggregationResponse;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveSearchCommands;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.search.SearchQueryResponse;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.search.SpellCheckResponse;
import io.quarkus.redis.datasource.search.SynDumpResponse;

public class BlockingSearchCommandsImpl<K> extends AbstractRedisCommandGroup implements SearchCommands<K> {

    private final ReactiveSearchCommands<K> reactive;

    public BlockingSearchCommandsImpl(RedisDataSource ds, ReactiveSearchCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public List<K> ft_list() {
        return reactive.ft_list().await().atMost(timeout);
    }

    @Override
    public AggregationResponse ftAggregate(String indexName, String query, AggregateArgs args) {
        return reactive.ftAggregate(indexName, query, args).await().atMost(timeout);
    }

    @Override
    public AggregationResponse ftAggregate(String indexName, String query) {
        return reactive.ftAggregate(indexName, query).await().atMost(timeout);
    }

    @Override
    public void ftAliasAdd(String alias, String index) {
        reactive.ftAliasAdd(alias, index).await().atMost(timeout);
    }

    @Override
    public void ftAliasDel(String alias) {
        reactive.ftAliasDel(alias).await().atMost(timeout);
    }

    @Override
    public void ftAliasUpdate(String alias, String index) {
        reactive.ftAliasUpdate(alias, index).await().atMost(timeout);
    }

    @Override
    public void ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        reactive.ftAlter(index, field, skipInitialScan).await().atMost(timeout);
    }

    @Override
    public void ftCreate(String index, CreateArgs args) {
        reactive.ftCreate(index, args).await().atMost(timeout);
    }

    @Override
    public void ftCursorDel(String index, long cursor) {
        reactive.ftCursorDel(index, cursor).await().atMost(timeout);
    }

    @Override
    public AggregationResponse ftCursorRead(String index, long cursor) {
        return reactive.ftCursorRead(index, cursor).await().atMost(timeout);
    }

    @Override
    public AggregationResponse ftCursorRead(String index, long cursor, int count) {
        return reactive.ftCursorRead(index, cursor, count).await().atMost(timeout);
    }

    @Override
    public void ftDropIndex(String index) {
        reactive.ftDropIndex(index).await().atMost(timeout);
    }

    @Override
    public void ftDropIndex(String index, boolean dd) {
        reactive.ftDropIndex(index, dd).await().atMost(timeout);
    }

    @Override
    public void ftDictAdd(String dict, String... words) {
        reactive.ftDictAdd(dict, words).await().atMost(timeout);
    }

    @Override
    public void ftDictDel(String dict, String... words) {
        reactive.ftDictDel(dict, words).await().atMost(timeout);
    }

    @Override
    public List<String> ftDictDump(String dict) {
        return reactive.ftDictDump(dict).await().atMost(timeout);
    }

    @Override
    public SearchQueryResponse ftSearch(String index, String query, QueryArgs args) {
        return reactive.ftSearch(index, query, args).await().atMost(timeout);
    }

    @Override
    public SearchQueryResponse ftSearch(String index, String query) {
        return reactive.ftSearch(index, query).await().atMost(timeout);
    }

    @Override
    public SpellCheckResponse ftSpellCheck(String index, String query) {
        return reactive.ftSpellCheck(index, query).await().atMost(timeout);
    }

    @Override
    public SpellCheckResponse ftSpellCheck(String index, String query, SpellCheckArgs args) {
        return reactive.ftSpellCheck(index, query, args).await().atMost(timeout);
    }

    @Override
    public SynDumpResponse ftSynDump(String index) {
        return reactive.ftSynDump(index).await().atMost(timeout);
    }

    @Override
    public void ftSynUpdate(String index, String groupId, String... words) {
        reactive.ftSynUpdate(index, groupId, words).await().atMost(timeout);
    }

    @Override
    public void ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        reactive.ftSynUpdate(index, groupId, skipInitialScan, words).await().atMost(timeout);
    }

    @Override
    public Set<String> ftTagVals(String index, String field) {
        return reactive.ftTagVals(index, field).await().atMost(timeout);
    }
}
