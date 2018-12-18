package org.jboss.panache;

import org.jboss.protean.arc.Arc;

import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public abstract class RxEntityBase<T extends RxEntityBase<?>> {
    
    protected abstract Object _getId();
    protected abstract void _setId(Object id);
    
    private static PgPool getPgPool() {
        return Arc.container().instance(PgPool.class).get();
    }

    public Single<? extends T> save() {
        PgPool pool = getPgPool();
        RxModelInfo<T> modelInfo = (RxModelInfo)getModelInfo();
        // FIXME: custom id generation
        if(_getId() == null)
            return pool.rxPreparedQuery("SELECT nextval('hibernate_sequence') AS id")
                    .map(rowset -> rowset.iterator().next().getInteger("id"))
                    .flatMap(id -> {
                        // non-persisted tuples are missing their id
                        Tuple t = modelInfo.toTuple((T)this);
                        Tuple withId = Tuple.tuple();
                        withId.addValue(id);
                        for (int i = 0; i < t.size(); i++) {
                            withId.addValue(t.getValue(i));
                        }
                        return pool.rxPreparedQuery(modelInfo.insertStatement(), withId)
                            .map(rowset -> {
                                _setId(id);
                                return (T)this;
                            });
                        });
        else
            return pool.rxPreparedQuery(modelInfo.updateStatement(), modelInfo.toTuple((T)this))
                    .map(rowset -> (T)this);
    }

    public Completable delete() {
        PgPool pool = getPgPool();
        // FIXME: id column from model info
        return pool.rxPreparedQuery("DELETE FROM "+getModelInfo().getTableName()+" WHERE id = $1", Tuple.of(_getId())).ignoreElement();
    }

    protected RxModelInfo<T> getModelInfo(){
        throw new RuntimeException("Should never be called");
    }
    
    //
    // Static Helpers
    
    public static <T extends RxEntityBase<?>> Observable<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    protected static <T extends RxEntityBase<?>> Observable<T> findAll(RxModelInfo<T> modelInfo) {
        PgPool pool = getPgPool();
        // FIXME: order by from model info
        return pool.rxQuery("SELECT * FROM "+modelInfo.getTableName()+" ORDER BY name")
                .flatMapObservable(rowset -> Observable.fromIterable(rowset.getDelegate()))
                .map(coreRow -> {
                    try {
                        return modelInfo.fromRow(Row.newInstance(coreRow));  
                    }catch(Throwable t) {
                        t.printStackTrace();
                        return null;
                    }
                });
    }

    public static <T extends RxEntityBase<?>> Maybe<T> findById(Integer id) {
        throw new RuntimeException("Should never be called");
    }
    
    protected static <T extends RxEntityBase<?>> Maybe<T> findById(RxModelInfo<T> modelInfo, Integer id) {
        PgPool pool = getPgPool();
        // FIXME: id column name from model info
        return pool.rxPreparedQuery("SELECT * FROM "+modelInfo.getTableName()+" WHERE id = $1", Tuple.of(id))
                .flatMapMaybe(rowset -> {
                    if(rowset.size() == 1)
                        return Maybe.just(rowset.iterator().next());
                    return Maybe.empty();
                })
                .map(row -> modelInfo.fromRow(row));
    }

    public interface RxModelInfo<T extends RxEntityBase<?>> {
        Class<T> getEntityClass();
        String getTableName();
        T fromRow(Row row);
        String insertStatement();
        String updateStatement();
        Tuple toTuple(T entity);
    }

}
