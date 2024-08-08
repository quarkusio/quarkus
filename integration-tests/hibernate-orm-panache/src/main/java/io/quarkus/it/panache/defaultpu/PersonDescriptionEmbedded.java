package io.quarkus.it.panache.defaultpu;

import jakarta.persistence.Embeddable;

@Embeddable
public class PersonDescriptionEmbedded {
    public String embeddedDescription;
}
