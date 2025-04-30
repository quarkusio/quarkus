package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = MyLoadingBinder.class))
@Indexed
public class LoadablePerson {

    @DocumentId
    private final long id;

    @FullTextField(analyzer = "standard")
    @KeywordField(name = "name_sort", normalizer = "lowercase", sortable = Sortable.YES)
    private String name;

    public LoadablePerson(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
