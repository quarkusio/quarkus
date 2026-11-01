package io.quarkus.redis.lettuce.runtime.internal.search;

import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalSearchCommandsImpl<K> implements ReactiveTransactionalSearchCommands {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveSearchCommandsImpl<K> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalSearchCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveSearchCommandsImpl<K> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> ft_list() {
        return tx.enqueue(reactive._ft_list());
    }

    @Override
    public Uni<Void> ftAggregate(String indexName, String query, AggregateArgs args) {
        return tx.enqueue(reactive._ftAggregate(indexName, query, args));
    }

    @Override
    public Uni<Void> ftAggregate(String indexName, String query) {
        return tx.enqueue(reactive._ftAggregate(indexName, query));
    }

    @Override
    public Uni<Void> ftAliasAdd(String alias, String index) {
        return tx.enqueue(reactive._ftAliasAdd(alias, index));
    }

    @Override
    public Uni<Void> ftAliasDel(String alias) {
        return tx.enqueue(reactive._ftAliasDel(alias));
    }

    @Override
    public Uni<Void> ftAliasUpdate(String alias, String index) {
        return tx.enqueue(reactive._ftAliasUpdate(alias, index));
    }

    @Override
    public Uni<Void> ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        return tx.enqueue(reactive._ftAlter(index, field, skipInitialScan));
    }

    @Override
    public Uni<Void> ftCreate(String index, CreateArgs args) {
        return tx.enqueue(reactive._ftCreate(index, args));
    }

    @Override
    public Uni<Void> ftCursorDel(String index, long cursor) {
        return tx.enqueue(reactive._ftCursorDel(index, cursor));
    }

    @Override
    public Uni<Void> ftCursorRead(String index, long cursor) {
        return tx.enqueue(reactive._ftCursorRead(index, cursor));
    }

    @Override
    public Uni<Void> ftCursorRead(String index, long cursor, int count) {
        return tx.enqueue(reactive._ftCursorRead(index, cursor, count));
    }

    @Override
    public Uni<Void> ftDropIndex(String index) {
        return tx.enqueue(reactive._ftDropIndex(index));
    }

    @Override
    public Uni<Void> ftDropIndex(String index, boolean dd) {
        return tx.enqueue(reactive._ftDropIndex(index, dd));
    }

    @Override
    public Uni<Void> ftDictAdd(String dict, String... words) {
        return tx.enqueue(reactive._ftDictAdd(dict, words));
    }

    @Override
    public Uni<Void> ftDictDel(String dict, String... words) {
        return tx.enqueue(reactive._ftDictDel(dict, words));
    }

    @Override
    public Uni<Void> ftDictDump(String dict) {
        return tx.enqueue(reactive._ftDictDump(dict));
    }

    @Override
    public Uni<Void> ftSearch(String index, String query, QueryArgs args) {
        return tx.enqueue(reactive._ftSearch(index, query, args));
    }

    @Override
    public Uni<Void> ftSearch(String index, String query) {
        return tx.enqueue(reactive._ftSearch(index, query));
    }

    @Override
    public Uni<Void> ftSpellCheck(String index, String query) {
        return tx.enqueue(reactive._ftSpellCheck(index, query));
    }

    @Override
    public Uni<Void> ftSpellCheck(String index, String query, SpellCheckArgs args) {
        return tx.enqueue(reactive._ftSpellCheck(index, query, args));
    }

    @Override
    public Uni<Void> ftSynDump(String index) {
        return tx.enqueue(reactive._ftSynDump(index));
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, String... words) {
        return tx.enqueue(reactive._ftSynUpdate(index, groupId, words));
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        return tx.enqueue(reactive._ftSynUpdate(index, groupId, skipInitialScan, words));
    }

    @Override
    public Uni<Void> ftTagVals(String index, String field) {
        return tx.enqueue(reactive._ftTagVals(index, field));
    }

}
