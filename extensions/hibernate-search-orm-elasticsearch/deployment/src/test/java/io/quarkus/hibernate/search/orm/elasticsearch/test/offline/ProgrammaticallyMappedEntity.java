package io.quarkus.hibernate.search.orm.elasticsearch.test.offline;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ProgrammaticallyMappedEntity {
    @Id
    public Long id;
}
