package io.quarkus.it.hibernate.search.orm.elasticsearch.propertyaccess;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MethodAccessEntity {

    private Long id;

    private String property;

    public MethodAccessEntity() {
    }

    public MethodAccessEntity(Long id, String property) {
        this.id = id;
        this.property = property;
    }

    @Id
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
}
