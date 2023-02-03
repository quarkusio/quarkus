package io.quarkus.it.hibernate.search.orm.elasticsearch.propertyaccess;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Entity
@Indexed
public class TransientMethodAccessEntity {

    private Long id;

    private String property1;

    private String property2;

    public TransientMethodAccessEntity() {
    }

    public TransientMethodAccessEntity(Long id, String property1, String property2) {
        this.id = id;
        this.property1 = property1;
        this.property2 = property2;
    }

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    public String getProperty2() {
        return property2;
    }

    public void setProperty2(String property2) {
        this.property2 = property2;
    }

    @Transient
    @FullTextField
    @IndexingDependency(derivedFrom = {
            @ObjectPath(@PropertyValue(propertyName = "property1")),
            @ObjectPath(@PropertyValue(propertyName = "property2"))
    })
    public String getProperty() {
        return property1 + " " + property2;
    }
}
