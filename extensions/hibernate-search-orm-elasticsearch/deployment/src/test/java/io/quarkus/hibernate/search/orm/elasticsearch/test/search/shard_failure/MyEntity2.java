package io.quarkus.hibernate.search.orm.elasticsearch.test.search.shard_failure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class MyEntity2 {

    @Id
    @GeneratedValue
    private Long id;

    @KeywordField
    private String text;

    public MyEntity2() {
    }

    public MyEntity2(String text) {
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
