package io.quarkus.hibernate.orm.envers;

import org.hibernate.envers.RevisionListener;

public class MyListenerlessRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        MyListenerlessRevisionEntity.class.cast(revisionEntity).setListenerValue(this.toString());
    }

}
