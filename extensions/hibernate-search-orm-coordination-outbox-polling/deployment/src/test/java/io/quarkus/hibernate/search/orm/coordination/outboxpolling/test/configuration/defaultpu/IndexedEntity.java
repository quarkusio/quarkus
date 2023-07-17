package io.quarkus.hibernate.search.orm.coordination.outboxpolling.test.configuration.defaultpu;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * An indexed entity.
 */
@Entity
@Indexed
public class IndexedEntity {

    @Id
    @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
