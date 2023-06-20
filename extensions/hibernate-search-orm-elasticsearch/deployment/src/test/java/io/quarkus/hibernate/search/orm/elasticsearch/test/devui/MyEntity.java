package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MyEntity {

    @Id
    Long id;

    @Column
    @FullTextField
    String field;

}
