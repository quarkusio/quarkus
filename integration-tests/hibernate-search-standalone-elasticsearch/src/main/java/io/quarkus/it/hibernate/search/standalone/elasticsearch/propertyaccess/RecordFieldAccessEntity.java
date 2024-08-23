package io.quarkus.it.hibernate.search.standalone.elasticsearch.propertyaccess;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public record RecordFieldAccessEntity(
        @DocumentId long id,
        @FullTextField String property,
        @FullTextField String otherProperty) {
}
