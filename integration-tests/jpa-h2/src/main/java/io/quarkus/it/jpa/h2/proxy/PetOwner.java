package io.quarkus.it.jpa.h2.proxy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class PetOwner {

    private Integer id;

    String name;

    PetProxy pet;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Id
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = Pet.class)
    @JoinColumn(nullable = false)
    public PetProxy getPet() {
        return pet;
    }

    public void setPet(PetProxy pet) {
        this.pet = pet;
    }
}
