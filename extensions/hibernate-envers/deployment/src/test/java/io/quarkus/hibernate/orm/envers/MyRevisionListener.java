package io.quarkus.hibernate.orm.envers;

import org.hibernate.envers.RevisionListener;

public class MyRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        MyRevisionEntity.class.cast(revisionEntity).setListenerValue(this.toString());
    }

}
