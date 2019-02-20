package org.jboss.panache.jpa;

import java.util.List;
import java.util.stream.Stream;

public class Query<Entity> {
    
    
    private javax.persistence.Query jpaQuery;
    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private int pageSize = Integer.MAX_VALUE;

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
        this.pageSize = pageSize;
        return (Query<T>) this;
    }
    
    // Results
    
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list(){
        jpaQuery.setMaxResults(pageSize);
        return jpaQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream(){
        jpaQuery.setMaxResults(pageSize);
        return jpaQuery.getResultStream();
    }
    
    public <T extends Entity> T getFirstResult() {
        List<T> list = list();
        jpaQuery.setMaxResults(1);
        return list.isEmpty() ? null : list.get(0);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Entity> T getSingleResult() {
        jpaQuery.setMaxResults(pageSize);
        return (T) jpaQuery.getSingleResult();
    }
}
