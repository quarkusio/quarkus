package io.quarkus.hibernate.orm.runtime.dev;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("dev")
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
