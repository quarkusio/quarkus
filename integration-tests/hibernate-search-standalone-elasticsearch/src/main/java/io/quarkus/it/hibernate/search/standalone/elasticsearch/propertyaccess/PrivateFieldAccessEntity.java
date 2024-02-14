package io.quarkus.it.hibernate.search.standalone.elasticsearch.propertyaccess;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class PrivateFieldAccessEntity {

    @DocumentId
    private Long id;

    @FullTextField
    private String property;

    @FullTextField
    private String otherProperty;

    public PrivateFieldAccessEntity(Long id, String property, String otherProperty) {
        this.id = id;
        this.property = property;
        this.otherProperty = otherProperty;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    public String property() {
        return property;
    }

    public void property(String property) {
        this.property = property;
    }

    public String otherProperty() {
        return otherProperty;
    }

    public void otherProperty(String otherProperty) {
        this.otherProperty = otherProperty;
    }
}
