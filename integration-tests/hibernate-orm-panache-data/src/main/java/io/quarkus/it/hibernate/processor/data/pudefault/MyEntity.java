package io.quarkus.it.hibernate.processor.data.pudefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

    public MyEntity() {
    }

    public MyEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue
    public Long id;

    @Column(unique = true)
    public String name;

    @Override
    public String toString() {
        return "MyOrmEntity [id=" + id + ", name=" + name + "]";
    }

}
