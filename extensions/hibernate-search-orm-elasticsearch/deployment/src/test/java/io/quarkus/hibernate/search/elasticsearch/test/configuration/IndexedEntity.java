package io.quarkus.hibernate.search.elasticsearch.test.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * An indexed entity.
 */
@Entity
@Indexed
public class IndexedEntity {

    @Id
    @GeneratedValue
    public Long id;

    @FullTextField
    public String name;

    protected IndexedEntity() {
    }

    public IndexedEntity(String name) {
        this.name = name;
    }

}
