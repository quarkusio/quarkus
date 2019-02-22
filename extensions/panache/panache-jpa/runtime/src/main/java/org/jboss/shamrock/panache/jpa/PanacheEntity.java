package io.quarkus.panache.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class PanacheEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    // FIXME: VERSION?
}
