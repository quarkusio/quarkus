package io.quarkus.hibernate.reactive.mapping.timezone;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithTimezones {

    @Id
    @GeneratedValue
    Long id;

    public EntityWithTimezones() {
    }

    public EntityWithTimezones(ZonedDateTime zonedDateTime, OffsetDateTime offsetDateTime, OffsetTime offsetTime) {
        this.zonedDateTime = zonedDateTime;
        this.offsetDateTime = offsetDateTime;
        this.offsetTime = offsetTime;
    }

    public ZonedDateTime zonedDateTime;

    public OffsetDateTime offsetDateTime;

    public OffsetTime offsetTime;

}
