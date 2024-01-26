package io.quarkus.it.hibernate.jpamodelgen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class MyStaticMetamodelEntity {

    @Id
    @GeneratedValue
    public Integer id;

    @Column(unique = true)
    public String name;

    MyStaticMetamodelEntity() {
    }

    public MyStaticMetamodelEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyOrmEntity [id=" + id + ", name=" + name + "]";
    }

}
