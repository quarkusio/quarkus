package org.jboss.panache.rx;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface RxDaoBase<Entity extends RxEntityBase<Entity>> {
    
    public default Single<? extends Entity> save(Entity entity) {
        return RxOperations.save(entity);
    }

    public default Completable delete(Entity entity) {
        return RxOperations.delete(entity);
    }

    //
    // Static Helpers
    
    public default Observable<Entity> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public default Maybe<Entity> findById(Object id) {
        throw new RuntimeException("Should never be called");
    }
    
    public default Observable<Entity> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> count(String query, Object...params) {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public default Single<Long> delete(String query, Object...params) {
        throw new RuntimeException("Should never be called");
    }
}
