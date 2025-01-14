package io.quarkus.hibernate.orm.runtime.dev.ui;

import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevController;
import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfo;

public class HibernateOrmDevJsonRpcService {

    public HibernateOrmDevInfo getInfo() {
        return HibernateOrmDevController.get().getInfo();
    }

    public int getNumberOfPersistenceUnits() {
        return getInfo().getPersistenceUnits().size();
    }

    public int getNumberOfEntityTypes() {
        return getInfo().getNumberOfEntities();
    }

    public int getNumberOfNamedQueries() {
        return getInfo().getNumberOfNamedQueries();
    }

}
