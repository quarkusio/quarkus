package io.quarkus.it.mongodb.panache.bugs;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

/**
 * An entity that have all the supported date format.
 * Asserting #6566 and possibility other date issues.
 */
public class DateEntity extends PanacheMongoEntity {
    public Date dateDate;
    public LocalDate localDate;
    public LocalDateTime localDateTime;
    public Instant instant;

    public DateEntity() {
        dateDate = new Date();
        localDate = LocalDate.now();
        localDateTime = LocalDateTime.now();
        instant = Instant.now();
    }
}
