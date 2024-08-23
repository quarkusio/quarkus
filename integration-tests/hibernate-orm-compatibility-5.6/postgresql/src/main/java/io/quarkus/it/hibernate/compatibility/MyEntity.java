package io.quarkus.it.hibernate.compatibility;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#basic-arraycollection-mapping
    // This mapping change is required because Quarkus cannot fix this through settings.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    public int[] intArray;

    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#basic-arraycollection-mapping
    // This mapping change is required because Quarkus cannot fix this through settings.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    public ArrayList<String> stringList;

    @Enumerated(EnumType.ORDINAL)
    public MyEnum myEnum;

}
