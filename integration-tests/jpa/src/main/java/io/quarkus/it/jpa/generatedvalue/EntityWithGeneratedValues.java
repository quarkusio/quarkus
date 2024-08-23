package io.quarkus.it.jpa.generatedvalue;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class EntityWithGeneratedValues {
    @Id
    @GeneratedValue
    public Integer id;

    @TenantId
    public String tenant;

    @CreationTimestamp
    public Instant creationTimestamp;

    @UpdateTimestamp
    public Instant updateTimestamp;

    @CurrentTimestamp
    public Instant currentTimestamp;

    @Generated
    @ColumnDefault("CURRENT_TIMESTAMP")
    public Instant generated;

    @GeneratedColumn("CURRENT_TIMESTAMP")
    public Instant generatedColumn;
}
