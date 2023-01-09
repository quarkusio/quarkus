package io.quarkus.it.jpa.h2.proxy;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import org.hibernate.annotations.Proxy;

@Inheritance()
@DiscriminatorColumn
@Proxy(proxyClass = PetProxy.class)
@DiscriminatorValue("PET")
@Entity
public class Pet implements PetProxy {

    private Integer id;

    private String name;

    @Id
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String makeNoise() {
        return "Generic pet noises";
    }
}
