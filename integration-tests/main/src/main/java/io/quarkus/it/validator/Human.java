package io.quarkus.it.validator;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.sun.istack.NotNull;

@Entity
@Table(name = "Human")
public class Human {

    @Id
    private int id;

    @NotEmpty
    private String name;

    @NotNull
    @Valid
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "greetedHuman")
    private Set<Hello> greetings = new HashSet<>();

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<Hello> getGreetings() {
        return greetings;
    }

    public void setGreetings(final Set<Hello> greetings) {
        this.greetings = greetings;
    }
}
