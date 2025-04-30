package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.persistence.Entity;

@Entity
public class MappingTestingSearchExtensionEntity extends MappingTestingEntityBase {

    public static final String INDEX = "mapping-testing-search-extension-entity";

    public MappingTestingSearchExtensionEntity() {
    }

    public MappingTestingSearchExtensionEntity(String text) {
        super(text);
    }
}
