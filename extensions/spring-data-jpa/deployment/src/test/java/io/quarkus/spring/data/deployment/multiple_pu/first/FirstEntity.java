package io.quarkus.spring.data.deployment.multiple_pu.first;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class FirstEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String name;
}
