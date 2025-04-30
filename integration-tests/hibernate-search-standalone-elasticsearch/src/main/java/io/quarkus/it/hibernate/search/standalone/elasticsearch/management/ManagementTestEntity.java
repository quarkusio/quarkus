package io.quarkus.it.hibernate.search.standalone.elasticsearch.management;

import java.util.UUID;

import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(loadingBinder = @EntityLoadingBinderRef(type = MyLoadingBinder.class))
@Indexed
public class ManagementTestEntity {

    @DocumentId
    private UUID id;

    @FullTextField
    private String name;

    public ManagementTestEntity() {
    }

    public ManagementTestEntity(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
