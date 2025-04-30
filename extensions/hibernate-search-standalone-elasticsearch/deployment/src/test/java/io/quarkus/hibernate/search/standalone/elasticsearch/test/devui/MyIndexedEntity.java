package io.quarkus.hibernate.search.standalone.elasticsearch.test.devui;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class MyIndexedEntity {

    @DocumentId
    Long id;

    @FullTextField
    String field;

}
