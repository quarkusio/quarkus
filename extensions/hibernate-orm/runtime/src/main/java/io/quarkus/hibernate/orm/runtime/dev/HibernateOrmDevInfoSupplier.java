package io.quarkus.hibernate.orm.runtime.dev;

import java.util.function.Supplier;

@Deprecated // Only useful for the legacy Dev UI
public class HibernateOrmDevInfoSupplier implements Supplier<HibernateOrmDevInfo> {

    @Override
    public HibernateOrmDevInfo get() {
        return HibernateOrmDevController.get().getInfo();
    }

}
