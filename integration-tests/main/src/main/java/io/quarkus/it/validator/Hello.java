package io.quarkus.it.validator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "Hello")
public class Hello {

    @Id
    private int id;

    @NotEmpty
    private String greetingText;

    @Valid
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(nullable = false)
    private Human greetedHuman;

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getGreetingText() {
        return greetingText;
    }

    public void setGreetingText(final String greetingText) {
        this.greetingText = greetingText;
    }

    public Human getGreetedHuman() {
        return greetedHuman;
    }

    public void setGreetedHuman(final Human greetedHuman) {
        this.greetedHuman = greetedHuman;
    }

}
