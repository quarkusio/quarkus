package io.quarkus.test.devconsole.namedpu;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MyNamedPuEntity {

    @Id
    Long id;

    String field;

}
