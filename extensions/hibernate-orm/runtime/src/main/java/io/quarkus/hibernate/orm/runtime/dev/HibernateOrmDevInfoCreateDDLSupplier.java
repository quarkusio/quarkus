package io.quarkus.hibernate.orm.runtime.dev;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.RecordableConstructor;

public class HibernateOrmDevInfoCreateDDLSupplier implements Supplier<String> {

    private final String puName;

    @RecordableConstructor
    public HibernateOrmDevInfoCreateDDLSupplier(String puName) {
        this.puName = puName;
    }

    @Override
    public String get() {
        Collection<HibernateOrmDevInfo.PersistenceUnit> persistenceUnits = HibernateOrmDevController.get().getInfo()
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
