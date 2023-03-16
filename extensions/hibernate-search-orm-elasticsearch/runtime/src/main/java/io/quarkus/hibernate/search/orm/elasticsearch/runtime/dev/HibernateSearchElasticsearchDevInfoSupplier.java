package io.quarkus.hibernate.search.orm.elasticsearch.runtime.dev;

import java.util.function.Supplier;

public class HibernateSearchElasticsearchDevInfoSupplier
        implements Supplier<HibernateSearchElasticsearchDevInfo> {

    @Override
    public HibernateSearchElasticsearchDevInfo get() {
        return HibernateSearchElasticsearchDevController.get().getInfo();
    }

}
