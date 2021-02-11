package io.quarkus.hibernate.search.orm.elasticsearch.runtime.devconsole;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import io.quarkus.arc.Arc;

public class HibernateSearchSupplier implements Supplier<List<String>> {
    @Override
    public List<String> get() {
        return searchMapping().allIndexedEntities().stream().map(SearchIndexedEntity::jpaName).sorted()
                .collect(Collectors.toList());

    }

    public static SearchMapping searchMapping() {
        return Arc.container().instance(SearchMapping.class).get();
    }
}
