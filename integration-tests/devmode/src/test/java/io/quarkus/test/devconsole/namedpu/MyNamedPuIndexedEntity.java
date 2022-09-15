package io.quarkus.test.devconsole.namedpu;

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
