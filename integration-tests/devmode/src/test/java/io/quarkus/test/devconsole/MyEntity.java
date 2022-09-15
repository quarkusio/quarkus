package io.quarkus.test.devconsole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

    @Id
    Long id;

    @Column
    String field;

}
