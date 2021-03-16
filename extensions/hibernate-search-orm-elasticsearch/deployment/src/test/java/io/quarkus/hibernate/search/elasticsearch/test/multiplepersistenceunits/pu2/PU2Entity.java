package io.quarkus.hibernate.search.elasticsearch.test.multiplepersistenceunits.pu2;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class PU2Entity {

    @Id
    @GeneratedValue
    private Long id;

    @FullTextField
    private String text;

    public PU2Entity() {
    }

    public PU2Entity(String text) {
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
