package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.search.TransactionalSearchCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalSearchCommandsImpl extends AbstractTransactionalRedisCommandGroup
        implements TransactionalSearchCommands {

    private final ReactiveTransactionalSearchCommands reactive;

    public BlockingTransactionalSearchCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalSearchCommands reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void ft_list() {
        reactive.ft_list().await().atMost(timeout);
    }

    @Override
    public void ftAggregate(String indexName, String query, AggregateArgs args) {
        reactive.ftAggregate(indexName, query, args).await().atMost(timeout);
    }

    @Override
    public void ftAggregate(String indexName, String query) {
        reactive.ftAggregate(indexName, query).await().atMost(timeout);
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
    public void ftAlter(String index, IndexedField field) {
        reactive.ftAlter(index, field).await().atMost(timeout);
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
    public void ftCursorRead(String index, long cursor) {
        reactive.ftCursorRead(index, cursor).await().atMost(timeout);
    }

    @Override
    public void ftCursorRead(String index, long cursor, int count) {
        reactive.ftCursorRead(index, cursor, count).await().atMost(timeout);
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
    public void ftDictDump(String dict) {
        reactive.ftDictDump(dict).await().atMost(timeout);
    }

    @Override
    public void ftSearch(String index, String query, QueryArgs args) {
        reactive.ftSearch(index, query, args).await().atMost(timeout);
    }

    @Override
    public void ftSearch(String index, String query) {
        reactive.ftSearch(index, query).await().atMost(timeout);
    }

    @Override
    public void ftSpellCheck(String index, String query) {
        reactive.ftSpellCheck(index, query).await().atMost(timeout);
    }

    @Override
    public void ftSpellCheck(String index, String query, SpellCheckArgs args) {
        reactive.ftSpellCheck(index, query, args).await().atMost(timeout);
    }

    @Override
    public void ftSynDump(String index) {
        reactive.ftSynDump(index).await().atMost(timeout);
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
    public void ftTagVals(String index, String field) {
        reactive.ftTagVals(index, field).await().atMost(timeout);
    }
}
