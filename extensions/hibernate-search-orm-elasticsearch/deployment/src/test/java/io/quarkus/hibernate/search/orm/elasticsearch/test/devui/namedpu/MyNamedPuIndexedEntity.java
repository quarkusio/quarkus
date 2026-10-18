package io.quarkus.hibernate.search.orm.elasticsearch.test.devui.namedpu;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class MyNamedPuIndexedEntity {

    @Id
    Long id;

    @FullTextField
    String field;

}
