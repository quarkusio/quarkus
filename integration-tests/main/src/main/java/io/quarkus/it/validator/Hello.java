package io.quarkus.it.validator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
