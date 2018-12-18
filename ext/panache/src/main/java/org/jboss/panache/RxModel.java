package org.jboss.panache;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@NotReallyJpa
@MappedSuperclass
public abstract class RxModel<T extends RxModel<T>> extends RxEntityBase<T> {
    
    @Id
    public Integer id;
    
    @Override
    protected Object _getId() {
        return id;
    }
    
    @Override
    protected void _setId(Object id) {
        this.id = (Integer)id;
    }
}
