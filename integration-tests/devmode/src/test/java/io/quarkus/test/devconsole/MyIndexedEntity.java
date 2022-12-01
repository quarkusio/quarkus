package io.quarkus.test.devconsole;

import javax.persistence.Entity;
import javax.persistence.Id;

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
