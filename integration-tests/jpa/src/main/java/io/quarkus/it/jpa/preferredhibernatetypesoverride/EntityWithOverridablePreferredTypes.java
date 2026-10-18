package io.quarkus.it.jpa.preferredhibernatetypesoverride;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = EntityWithOverridablePreferredTypes.NAME)
public class EntityWithOverridablePreferredTypes {
    public static final String NAME = "ent_with_preferred_types";

    @Id
    @GeneratedValue
    public UUID id;

    public Instant createdAt;

    public boolean isPersisted;

    public Duration overridenDuration;
}
