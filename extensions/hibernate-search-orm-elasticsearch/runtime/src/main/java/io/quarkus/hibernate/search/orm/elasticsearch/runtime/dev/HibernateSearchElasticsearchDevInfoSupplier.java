package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.function.Supplier;

@Deprecated // Only useful for the legacy Dev UI
public class HibernateSearchElasticsearchDevInfoSupplier
        implements Supplier<HibernateSearchElasticsearchDevInfo> {

    @Override
    public HibernateSearchElasticsearchDevInfo get() {
        return HibernateSearchElasticsearchDevController.get().getInfo();
    }

}
