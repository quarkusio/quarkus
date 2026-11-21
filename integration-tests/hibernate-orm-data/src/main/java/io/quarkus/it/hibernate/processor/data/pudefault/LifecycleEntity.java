package io.quarkus.it.hibernate.processor.data.pudefault;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class LifecycleEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String __quarkusLifecyclePreInsert;
}
