package io.quarkus.it.hibernate.search.standalone.elasticsearch.propertyaccess;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class PublicFieldAccessEntity {
    @DocumentId
    public Long id;

    @FullTextField
    public String property;

    @FullTextField
    public String otherProperty;

    public PublicFieldAccessEntity(long id, String property, String otherProperty) {
        this.id = id;
        this.property = property;
        this.otherProperty = otherProperty;
    }
}
