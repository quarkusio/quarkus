package io.quarkus.it.hibernate.compatibility;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    public Long id;

    public Duration duration;

    public UUID uuid;

    public Instant instant;

    public OffsetTime offsetTime;

    public OffsetDateTime offsetDateTime;

    public ZonedDateTime zonedDateTime;

    public int[] intArray;

    public ArrayList<String> stringList;

    @Enumerated(EnumType.ORDINAL)
    public MyEnum myEnum;

}
