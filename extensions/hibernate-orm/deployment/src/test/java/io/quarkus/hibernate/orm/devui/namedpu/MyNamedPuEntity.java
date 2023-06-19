package io.quarkus.hibernate.orm.devui.namedpu;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyNamedPuEntity {

    @Id
    Long id;

    String field;

}
