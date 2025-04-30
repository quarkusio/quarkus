package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.persistence.Entity;

@Entity
public class MappingTestingDependentBeanEntity extends MappingTestingEntityBase {

    public static final String INDEX = "mapping-testing-dependent-bean-entity";

    public MappingTestingDependentBeanEntity() {
    }

    public MappingTestingDependentBeanEntity(String text) {
        super(text);
    }
}
