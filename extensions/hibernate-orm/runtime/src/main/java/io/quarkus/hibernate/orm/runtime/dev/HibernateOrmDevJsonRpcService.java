package io.quarkus.hibernate.orm.runtime.dev;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

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
