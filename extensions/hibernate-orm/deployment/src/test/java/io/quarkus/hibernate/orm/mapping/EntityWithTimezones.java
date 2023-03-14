package io.quarkus.hibernate.orm.mapping;

import java.time.OffsetDateTime;
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

    public EntityWithTimezones(ZonedDateTime zonedDateTime, OffsetDateTime offsetDateTime) {
        this.zonedDateTime = zonedDateTime;
        this.offsetDateTime = offsetDateTime;
    }

    public ZonedDateTime zonedDateTime;

    public OffsetDateTime offsetDateTime;

}
