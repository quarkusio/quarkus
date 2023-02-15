package io.quarkus.spring.data.deployment.multiple_pu.second;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class SecondEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String name;
}
