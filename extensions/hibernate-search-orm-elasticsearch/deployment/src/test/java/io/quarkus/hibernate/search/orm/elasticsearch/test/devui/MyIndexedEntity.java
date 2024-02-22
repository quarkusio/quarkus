package io.quarkus.hibernate.search.orm.elasticsearch.test.devui;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MyIndexedEntity {

    @Id
    Long id;

    @FullTextField
    String field;

}
