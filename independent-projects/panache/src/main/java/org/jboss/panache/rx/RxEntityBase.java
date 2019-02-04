package org.jboss.panache.rx;

import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public abstract class RxEntityBase<T extends RxEntityBase<?>> {
    
    protected abstract Object _getId();
    protected abstract void _setId(Object id);
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Single<? extends T> save() {
        return (Single) RxOperations.save(this);
    }

    public Completable delete() {
        return RxOperations.delete(this);
    }

    protected RxModelInfo<T> getModelInfo(){
        throw new RuntimeException("Should never be called");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(obj.getClass() != getClass())
            return false;
        return Objects.equals(_getId(), ((RxEntityBase)obj)._getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(_getId());
    }

    //
    // Static Helpers
    
    public static <T extends RxEntityBase<?>> Observable<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    public static <T extends RxEntityBase<?>> Maybe<T> findById(Object id) {
        throw new RuntimeException("Should never be called");
    }
    
    public static <T extends RxEntityBase<?>> Observable<T> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> count() {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> count(String query, Object...params) {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> deleteAll() {
        throw new RuntimeException("Should never be called");
    }

    public static Single<Long> delete(String query, Object...params) {
        throw new RuntimeException("Should never be called");
    }
}
