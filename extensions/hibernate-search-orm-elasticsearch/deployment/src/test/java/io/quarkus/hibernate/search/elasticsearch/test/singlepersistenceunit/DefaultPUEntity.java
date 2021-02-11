package io.quarkus.hibernate.search.elasticsearch.test.singlepersistenceunit;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class DefaultPUEntity {

    @Id
    @GeneratedValue
    private Long id;

    @FullTextField
    private String text;

    public DefaultPUEntity() {
    }

    public DefaultPUEntity(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
