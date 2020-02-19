package io.quarkus.hibernate.orm.entitylistener;

import javax.inject.Inject;
import javax.persistence.PrePersist;

public class FooListener {

    @Inject
    FooBean fooBean;

    public FooListener() {

    }

    @PrePersist
    public void prePersist(FooEntity entity) {
        String data = this.fooBean.pleaseDoNotCrash();
        entity.setData(data);
    }
}
