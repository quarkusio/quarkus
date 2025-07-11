package io.quarkus.it.hibernate.processor.data.puother;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class MyOtherEntity {

    @Id
    @GeneratedValue
    public Integer id;

    @Column(unique = true)
    public String name;

    MyOtherEntity() {
    }

    public MyOtherEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyOrmEntity [id=" + id + ", name=" + name + "]";
    }

}
