package org.jboss.panache.rx;

import org.jboss.panache.rx.RxEntityBase.RxModelInfo;
import org.jboss.protean.arc.Arc;

import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public class RxOperations {

    // 
    // instance methods
    
    public static <T extends RxEntityBase<?>> Single<? extends T> save(T entity) {
        PgPool pool = getPgPool();
        RxModelInfo<T> modelInfo = (RxModelInfo)entity.getModelInfo();
        // FIXME: custom id generation
        return modelInfo.toTuple(entity)
                .flatMap(t -> {
                    if(entity._getId() == null)
                        return pool.rxPreparedQuery("SELECT nextval('hibernate_sequence') AS id")
                                .map(rowset -> rowset.iterator().next().getInteger("id"))
                                .flatMap(id -> {
                                    // non-persisted tuples are missing their id
                                    Tuple withId = Tuple.tuple();
                                    withId.addValue(id);
                                    for (int i = 0; i < t.size(); i++) {
                                        withId.addValue(t.getValue(i));
                                    }
                                    return pool.rxPreparedQuery(modelInfo.insertStatement(), withId)
                                            .map(rowset -> {
                                                entity._setId(id);
                                                return entity;
                                            });
                                });
                    else
                        return pool.rxPreparedQuery(modelInfo.updateStatement(), t)
                                .map(rowset -> entity);
                });
    }

    public static <T extends RxEntityBase<?>> Completable delete(T entity) {
        PgPool pool = getPgPool();
        // FIXME: id column from model info
        return pool.rxPreparedQuery("DELETE FROM "+entity.getModelInfo().getTableName()+" WHERE id = $1", Tuple.of(entity._getId())).ignoreElement();
    }

    //
    // Private stuff
    
    private static PgPool getPgPool() {
        return Arc.container().instance(PgPool.class).get();
    }

    private static Tuple toParams(Object[] params) {
        Tuple t = Tuple.tuple();
        for (Object param : params) {
            t.addValue(param);
        }
        return t;
    }

    private static String createFindQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        // FIXME: field order from model info
        if(query == null)
            return "SELECT * FROM "+getEntityName(modelInfo);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "SELECT * FROM "+getEntityName(modelInfo);
        
        String lc = query.toLowerCase();
        String translatedQuery = translateQuery(query);
        if(lc.startsWith("from ")) {
            return "SELECT * " + translatedQuery;
        }
        if(lc.startsWith("select ")) {
            throw new IllegalArgumentException("Select queries not yet supported");
        }
        if(lc.startsWith("order by ")) {
            return "SELECT * FROM "+getEntityName(modelInfo) + " " + translatedQuery;
        }
        return "SELECT * FROM "+getEntityName(modelInfo)+" WHERE "+translatedQuery;
    }

    private static String translateQuery(String query) {
        return query.replaceAll("\\?(\\d+)", "\\$$1");
    }

    private static String createCountQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        if(query == null)
            return "SELECT COUNT(*) FROM "+getEntityName(modelInfo);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "SELECT COUNT(*) FROM "+getEntityName(modelInfo);
        
        String lc = query.toLowerCase();
        String translatedQuery = translateQuery(query);
        if(lc.startsWith("from ")) {
            return "SELECT COUNT(*) "+translatedQuery;
        }
        if(lc.startsWith("order by ")) {
            // ignore it
            return "SELECT COUNT(*) FROM "+getEntityName(modelInfo);
        }
        return "SELECT COUNT(*) FROM "+getEntityName(modelInfo)+" WHERE "+translatedQuery;
    }

    private static String createDeleteQuery(RxModelInfo<?> modelInfo, String query, Object[] params) {
        if(query == null)
            return "DELETE FROM "+getEntityName(modelInfo);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "DELETE FROM "+getEntityName(modelInfo);
        
        String lc = query.toLowerCase();
        String translatedQuery = translateQuery(query);
        if(lc.startsWith("from ")) {
            return "DELETE "+translatedQuery;
        }
        if(lc.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM "+getEntityName(modelInfo);
        }
        return "DELETE FROM "+getEntityName(modelInfo)+" WHERE "+translatedQuery;
    }
    
    private static String getEntityName(RxModelInfo<?> modelInfo) {
        return modelInfo.getTableName();
    }

    //
    // Static Helpers
    
    public static <T extends RxEntityBase<?>> Observable<T> findAll(RxModelInfo<T> modelInfo) {
        PgPool pool = getPgPool();
        // FIXME: field list and order by from model info
        return pool.rxQuery("SELECT * FROM "+modelInfo.getTableName()+" ORDER BY name")
                .flatMapObservable(rowset -> Observable.fromIterable(rowset.getDelegate()))
                .map(coreRow -> {
                    try {
                        Row row = Row.newInstance(coreRow);
                        T t =  modelInfo.fromRow(row);
                        return t;
                    }catch(Throwable t) {
                        t.printStackTrace();
                        return null;
                    }
                });
    }

    public static <T extends RxEntityBase<?>> Maybe<T> findById(RxModelInfo<T> modelInfo, Object id) {
        PgPool pool = getPgPool();
        // FIXME: field list and id column name from model info
        return pool.rxPreparedQuery("SELECT * FROM "+modelInfo.getTableName()+" WHERE id = $1", Tuple.of(id))
                .flatMapMaybe(rowset -> {
                    if(rowset.size() == 1)
                        return Maybe.just(rowset.iterator().next());
                    return Maybe.empty();
                })
                .map(row -> modelInfo.fromRow(row));
    }

    public static <T extends RxEntityBase<?>> Observable<T> find(RxModelInfo<T> modelInfo, String query, Object... params) {
        PgPool pool = getPgPool();
        // FIXME: order by from model info
        return pool.rxPreparedQuery(createFindQuery(modelInfo, query, params), toParams(params))
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

    public static Single<Long> count(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.rxQuery("SELECT COUNT(*) FROM "+modelInfo.getTableName())
                .map(rowset -> rowset.iterator().next().getLong(0));
    }

    public static Single<Long> count(RxModelInfo<?> modelInfo, String query, Object...params) {
        PgPool pool = getPgPool();
        return pool.rxPreparedQuery(createCountQuery(modelInfo, query, params), toParams(params))
                .map(rowset -> rowset.iterator().next().getLong(0));
    }

    public static Single<Long> deleteAll(RxModelInfo<?> modelInfo) {
        PgPool pool = getPgPool();
        return pool.rxQuery("DELETE FROM "+modelInfo.getTableName())
                .map(rowset -> (long)rowset.rowCount());
    }

    public static Single<Long> delete(RxModelInfo<?> modelInfo, String query, Object...params) {
        PgPool pool = getPgPool();
        return pool.rxPreparedQuery(createDeleteQuery(modelInfo, query, params), toParams(params))
                .map(rowset -> (long)rowset.rowCount());
    }
}
