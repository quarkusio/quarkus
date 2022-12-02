package io.quarkus.it.jpa.h2.basicproxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("Concrete")
public class ConcreteEntity extends AbstractEntity {

    public ConcreteEntity() {
        super();
    }

}