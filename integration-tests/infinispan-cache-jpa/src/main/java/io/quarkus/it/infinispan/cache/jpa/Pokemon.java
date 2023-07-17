package io.quarkus.it.infinispan.cache.jpa;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Cacheable
public class Pokemon {

    private int id;
    private String name;
    private int cp;

    public Pokemon() {
    }

    public Pokemon(int id, String name, int cp) {
        this.id = id;
        this.cp = cp;
        this.name = name;
    }

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCp() {
        return cp;
    }

    public void setCp(int cp) {
        this.cp = cp;
    }

}
