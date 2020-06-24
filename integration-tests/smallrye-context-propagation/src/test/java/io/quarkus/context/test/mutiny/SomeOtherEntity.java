package io.quarkus.context.test.mutiny;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SomeOtherEntity extends PanacheEntity {

    public String name;

}
