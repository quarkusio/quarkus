package io.quarkus.hibernate.search.standalone.elasticsearch.test.offline;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class IndexedEntity {

    @DocumentId
    public Long id;

}
