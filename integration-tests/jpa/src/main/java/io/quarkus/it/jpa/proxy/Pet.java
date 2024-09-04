package io.quarkus.it.jpa.proxy;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

@Inheritance()
@DiscriminatorColumn
@DiscriminatorValue("PET")
@Entity
public class Pet {

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

    public String makeNoise() {
        return "Generic pet noises";
    }
}
