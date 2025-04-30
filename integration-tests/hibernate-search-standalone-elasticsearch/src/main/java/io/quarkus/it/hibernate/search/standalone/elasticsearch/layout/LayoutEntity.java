package io.quarkus.it.hibernate.search.standalone.elasticsearch.layout;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class LayoutEntity {

    @DocumentId
    private Long id;

    @FullTextField(analyzer = "standard")
    @KeywordField(name = "name_sort", normalizer = "lowercase", sortable = Sortable.YES)
    private String name;

    public LayoutEntity() {
    }

    public LayoutEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
