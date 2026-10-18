package io.quarkus.hibernate.orm.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Audited;

@Audited
@Entity(name = "AuditedTestEntity")
public class AuditedTestEntity {

    @Id
    long id;

    String title;

    public AuditedTestEntity() {
    }

    public AuditedTestEntity(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
