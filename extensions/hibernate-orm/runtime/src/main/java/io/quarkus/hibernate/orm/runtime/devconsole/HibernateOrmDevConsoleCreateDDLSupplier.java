package io.quarkus.hibernate.orm.runtime.devconsole;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class HibernateOrmDevConsoleCreateDDLSupplier implements Supplier<String> {

    private final String puName;

    @RecordableConstructor
    public HibernateOrmDevConsoleCreateDDLSupplier(String puName) {
        this.puName = puName;
    }

    @Override
    public String get() {
        Collection<HibernateOrmDevConsoleInfoSupplier.PersistenceUnitInfo> persistenceUnits = HibernateOrmDevConsoleInfoSupplier.INSTANCE
                .getPersistenceUnits();
        for (var p : persistenceUnits) {
            if (Objects.equals(puName, p.getName())) {
                return p.getCreateDDL();
            }
        }
        return null;
    }

    public String getPuName() {
        return puName;
    }
}
