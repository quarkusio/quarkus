package io.quarkus.it.hibernate.search.orm.elasticsearch.propertyaccess;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class PrivateFieldAccessEntity {

    @Id
    private Long id;

    @Basic(fetch = FetchType.LAZY)
    @LazyGroup("group1")
    @FullTextField
    private String property;

    @Basic(fetch = FetchType.LAZY)
    @LazyGroup("group2")
    @FullTextField
    private String otherProperty;

    public PrivateFieldAccessEntity() {
    }

    public PrivateFieldAccessEntity(Long id, String property) {
        this.id = id;
        this.property = property;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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
