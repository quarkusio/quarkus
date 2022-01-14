package io.quarkus.logging;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class LoggingEntity extends PanacheEntity {
    public void something() {
        Log.info("Hi!");
    }
}
