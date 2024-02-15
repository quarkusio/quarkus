package io.quarkus.it.panache;

import jakarta.persistence.Embeddable;

@Embeddable
public class PersonDescriptionEmbedded {
    public String embeddedDescription;
}
