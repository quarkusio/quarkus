package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

/**
 * An indexed entity.
 */
@SearchEntity
@Indexed
public class IndexedEntity {

    @DocumentId
    public Long id;

    @FullTextField
    public String name;

    public IndexedEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
