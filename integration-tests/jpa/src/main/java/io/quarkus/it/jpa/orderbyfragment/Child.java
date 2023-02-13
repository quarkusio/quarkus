package io.quarkus.it.jpa.orderbyfragment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Child {

    @Id
    private Long id;

    @ManyToOne
    private Parent parent;

    private String forename;

    private String favoritePet;

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getFavoritePet() {
        return favoritePet;
    }

    public void setFavoritePet(String favoritePet) {
        this.favoritePet = favoritePet;
    }

}
