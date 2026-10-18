package io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.subpackage;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class OtherUserInSubPackage {

    private long id;

    private String name;

    public OtherUserInSubPackage() {
    }

    public OtherUserInSubPackage(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSeq")
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

    @Override
    public String toString() {
        return "User:" + name;
    }
}
