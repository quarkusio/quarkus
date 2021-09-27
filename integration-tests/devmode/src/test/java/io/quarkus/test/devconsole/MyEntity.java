package io.quarkus.test.devconsole;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MyEntity {

    @Id
    Long id;

    @Column
    String field;

}
