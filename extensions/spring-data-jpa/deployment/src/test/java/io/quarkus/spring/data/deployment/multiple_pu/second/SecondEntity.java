package io.quarkus.spring.data.deployment.multiple_pu.second;

import jakarta.persistence.*;

@Entity
public class SecondEntity {

    @Id
    @GeneratedValue
    public Long id;

    public String name;

    @OneToOne(cascade = CascadeType.ALL)
    public SecondEntity child;
}
