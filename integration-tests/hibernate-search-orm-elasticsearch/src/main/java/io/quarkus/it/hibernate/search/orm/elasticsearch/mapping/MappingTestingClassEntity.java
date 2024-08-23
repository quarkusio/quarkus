package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.persistence.Entity;

@Entity
public class MappingTestingClassEntity extends MappingTestingEntityBase {

    public static final String INDEX = "mapping-testing-class-entity";

    public MappingTestingClassEntity() {
    }

    public MappingTestingClassEntity(String text) {
        super(text);
    }
}
