package io.quarkus.it.audit;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Changelog;

@Changelog
@Entity
public class RevisionInfo {

    @Id
    @GeneratedValue
    @Changelog.ChangesetId
    int id;

    @Changelog.Timestamp
    Instant timestamp;

    public int getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
