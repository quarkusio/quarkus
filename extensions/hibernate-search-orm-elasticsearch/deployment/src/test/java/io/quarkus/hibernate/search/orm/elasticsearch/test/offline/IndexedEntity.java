package io.quarkus.hibernate.search.orm.elasticsearch.test.offline;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class IndexedEntity {

    @Id
    @GeneratedValue
    public Long id;

}
