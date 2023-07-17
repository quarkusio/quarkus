package io.quarkus.it.infinispan.cache.jpa;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
public class Trainer {

    private long id;
    private List<Pokemon> pokemons;

    public Trainer() {
    }

    public Trainer(Pokemon... pokemons) {
        this.pokemons = Arrays.asList(pokemons);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trainerSeq")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<Pokemon> getPokemons() {
        return pokemons;
    }

    public void setPokemons(List<Pokemon> pokemons) {
        this.pokemons = pokemons;
    }

}
