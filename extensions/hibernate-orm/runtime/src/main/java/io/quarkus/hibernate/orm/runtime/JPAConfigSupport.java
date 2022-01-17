package io.quarkus.hibernate.orm.runtime;

import java.util.Set;

public class JPAConfigSupport {

    public Set<String> persistenceUnitNames;

    public JPAConfigSupport() {
    }

    public JPAConfigSupport(Set<String> persistenceUnitNames) {
        this.persistenceUnitNames = persistenceUnitNames;
    }
}
