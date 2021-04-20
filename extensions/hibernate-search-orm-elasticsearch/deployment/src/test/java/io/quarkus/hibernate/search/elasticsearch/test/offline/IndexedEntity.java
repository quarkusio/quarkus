package io.quarkus.hibernate.search.elasticsearch.test.offline;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class IndexedEntity {

    @Id
    @GeneratedValue
    public Long id;

}
