package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.persistence.Entity;

@Entity
public class MappingTestingApplicationBeanEntity extends MappingTestingEntityBase {

    public static final String INDEX = "mapping-testing-application-bean-entity";

    public MappingTestingApplicationBeanEntity() {
    }

    public MappingTestingApplicationBeanEntity(String text) {
        super(text);
    }
}
