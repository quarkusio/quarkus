package io.quarkus.it.jpa.h2.basicproxy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("Concrete")
public class ConcreteEntity extends AbstractEntity {

    public ConcreteEntity() {
        super();
    }

}
