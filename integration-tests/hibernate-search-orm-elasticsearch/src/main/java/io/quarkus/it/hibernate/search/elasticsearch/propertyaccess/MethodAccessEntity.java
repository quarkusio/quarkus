package io.quarkus.it.hibernate.search.elasticsearch.propertyaccess;

import javax.persistence.Entity;
import javax.persistence.Id;

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
