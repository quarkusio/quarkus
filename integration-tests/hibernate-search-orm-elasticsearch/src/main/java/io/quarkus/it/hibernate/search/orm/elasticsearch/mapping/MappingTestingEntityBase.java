package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class MappingTestingEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mappingSeq")
    private Long id;

    private String text;

    public MappingTestingEntityBase() {
    }

    public MappingTestingEntityBase(String text) {
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
