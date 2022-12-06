package io.quarkus.hibernate.orm.envers;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class MyAuditedVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myAuditedEntitySeq")
    private long id;

    private String name;

    @Version
    private long version;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getVersion() {
        return version;
    }
}
