package io.quarkus.hibernate.orm.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.SoftDelete;

@SoftDelete
@Entity
public class SoftDeleteEntity {

    @Id
    @GeneratedValue
    Long id;

    String name;

    public SoftDeleteEntity() {
    }

    public SoftDeleteEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
