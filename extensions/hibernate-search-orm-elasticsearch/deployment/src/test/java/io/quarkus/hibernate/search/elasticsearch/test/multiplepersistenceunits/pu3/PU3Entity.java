package io.quarkus.hibernate.search.elasticsearch.test.multiplepersistenceunits.pu3;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class PU3Entity {

    @Id
    @GeneratedValue
    private Long id;

    private String text;

    public PU3Entity() {
    }

    public PU3Entity(String text) {
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
