package org.jboss.panache.jpa;

import java.util.List;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

public class Query<Entity> {
    
    
    private javax.persistence.Query jpaQuery;

    public Query(javax.persistence.Query jpaQuery) {
        this.jpaQuery = jpaQuery;
    }

    // Builder
    
    public <T extends Entity> Query<T> page(Page page){
        return page(page.index, page.size);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Entity> Query<T> page(int pageIndex, int pageSize){
        jpaQuery.setFirstResult(pageIndex * pageSize);
        jpaQuery.setMaxResults(pageSize);
        return (Query<T>) this;
    }
    
    // Results
    
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list(){
        return jpaQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream(){
        return jpaQuery.getResultStream();
    }
    
    public <T extends Entity> T getFirstResult() {
        // FIXME: force max results for better perf?
        List<T> list = list();
        // FIXME: do not throw? Or variant to not throw?
        if(list.isEmpty())
            throw new NoResultException("No first result");
        return list.get(0);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Entity> T getSingleResult() {
        return (T) jpaQuery.getSingleResult();
    }
}
