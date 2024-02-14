package io.quarkus.it.hibernate.search.standalone.elasticsearch.propertyaccess;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class MethodAccessEntity {

    private Long id;

    public String property;

    @FullTextField // Annotation placement should not matter: if a getter exist, we use it
    public String otherProperty;

    public MethodAccessEntity(Long id, String property, String otherProperty) {
        this.id = id;
        this.property = property;
        this.otherProperty = otherProperty;
    }

    @DocumentId
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @FullTextField
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getOtherProperty() {
        return otherProperty;
    }

    public void setOtherProperty(String otherProperty) {
        this.otherProperty = otherProperty;
    }
}
