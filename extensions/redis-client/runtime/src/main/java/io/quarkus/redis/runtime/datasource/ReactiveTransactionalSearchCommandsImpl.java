package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;

public class ReactiveTransactionalSearchCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalSearchCommands {

    private final ReactiveSearchCommandsImpl<K> reactive;

    public ReactiveTransactionalSearchCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveSearchCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> ft_list() {
        tx.enqueue(res -> reactive.marshaller.decodeAsList(res, reactive.keyType));
        return this.reactive._ft_list().invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAggregate(String indexName, String query, AggregateArgs args) {
        tx.enqueue(r -> reactive.decodeAggregateResponse(r, args.hasCursor()));
        return this.reactive._ftAggregate(indexName, query, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAggregate(String indexName, String query) {
        tx.enqueue(r -> reactive.decodeAggregateResponse(r, false));

        return this.reactive._ftAggregate(indexName, query).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAliasAdd(String alias, String index) {
        tx.enqueue(r -> null);
        return this.reactive._ftAliasAdd(alias, index).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAliasDel(String alias) {
        tx.enqueue(r -> null);
        return this.reactive._ftAliasDel(alias).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAliasUpdate(String alias, String index) {
        tx.enqueue(r -> null);
        return this.reactive._ftAliasUpdate(alias, index).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAlter(String index, IndexedField field, boolean skipInitialScan) {
        tx.enqueue(r -> null);
        return this.reactive._ftAlter(index, field, skipInitialScan).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftAlter(String index, IndexedField field) {
        return ftAlter(index, field, false);
    }

    @Override
    public Uni<Void> ftCreate(String index, CreateArgs args) {
        tx.enqueue(r -> null);
        return this.reactive._ftCreate(index, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftCursorDel(String index, long cursor) {
        tx.enqueue(r -> null);
        return this.reactive._ftCursorDel(index, cursor).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftCursorRead(String index, long cursor) {
        tx.enqueue(r -> null);
        return this.reactive._ftCursorRead(index, cursor).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftCursorRead(String index, long cursor, int count) {
        tx.enqueue(r -> null);
        return this.reactive._ftCursorRead(index, cursor, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDropIndex(String index) {
        tx.enqueue(r -> null);
        return this.reactive._ftDropIndex(index).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDropIndex(String index, boolean dd) {
        tx.enqueue(r -> null);
        return this.reactive._ftDropIndex(index, dd).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDictAdd(String dict, String... words) {
        tx.enqueue(r -> null);
        return this.reactive._ftDictAdd(dict, words).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDictDel(String dict, String... words) {
        tx.enqueue(r -> null);
        return this.reactive._ftDictDel(dict, words).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftDictDump(String dict) {
        tx.enqueue(r -> reactive.marshaller.decodeAsList(r, String.class));
        return this.reactive._ftDictDump(dict).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSearch(String index, String query, QueryArgs args) {
        tx.enqueue(r -> reactive.decodeSearchQueryResult(r, args));
        return this.reactive._ftSearch(index, query, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSearch(String index, String query) {
        tx.enqueue(r -> reactive.decodeSearchQueryResult(r, null));
        return this.reactive._ftSearch(index, query).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSpellCheck(String index, String query) {
        tx.enqueue(reactive::decodeSpellcheckResponse);
        return this.reactive._ftSpellCheck(index, query).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSpellCheck(String index, String query, SpellCheckArgs args) {
        tx.enqueue(reactive::decodeSpellcheckResponse);
        return this.reactive._ftSpellCheck(index, query, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSynDump(String index) {
        tx.enqueue(reactive::decodeSynDumpResponse);
        return this.reactive._ftSynDump(index).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, String... words) {
        tx.enqueue(r -> null);
        return this.reactive._ftSynUpdate(index, groupId, words).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSynUpdate(String index, String groupId, boolean skipInitialScan, String... words) {
        tx.enqueue(r -> null);
        return this.reactive._ftSynUpdate(index, groupId, skipInitialScan, words).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> ftTagVals(String index, String field) {
        tx.enqueue(r -> reactive.marshaller.decodeAsSet(r, String.class));
        return this.reactive._ftTagVals(index, field).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
