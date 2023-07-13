package io.quarkus.test.devui;

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
