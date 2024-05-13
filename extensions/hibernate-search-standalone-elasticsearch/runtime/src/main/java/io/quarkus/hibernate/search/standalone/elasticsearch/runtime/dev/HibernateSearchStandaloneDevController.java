package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.dev;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import io.quarkus.arc.Arc;

public class HibernateSearchStandaloneDevController {

    private static final HibernateSearchStandaloneDevController INSTANCE = new HibernateSearchStandaloneDevController();

    public static HibernateSearchStandaloneDevController get() {
        return INSTANCE;
    }

    private boolean active;

    private HibernateSearchStandaloneDevController() {
    }

    void setActive(boolean active) {
        this.active = active;
    }

    public HibernateSearchStandaloneDevInfo getInfo() {
        if (!active) {
            return new HibernateSearchStandaloneDevInfo(List.of());
        }
        return new HibernateSearchStandaloneDevInfo(searchMapping().allIndexedEntities().stream()
                .map(HibernateSearchStandaloneDevInfo.IndexedEntity::new).sorted()
                .collect(Collectors.toList()));
    }

    public SearchMapping searchMapping() {
        return Arc.container()
                .select(SearchMapping.class)
                .get();
    }

}
