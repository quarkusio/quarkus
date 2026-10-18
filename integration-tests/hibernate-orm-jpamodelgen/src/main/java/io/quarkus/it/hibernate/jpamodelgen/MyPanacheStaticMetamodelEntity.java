package io.quarkus.it.hibernate.jpamodelgen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class MyPanacheStaticMetamodelEntity extends PanacheEntity {

    @Column(unique = true)
    public String name;

    MyPanacheStaticMetamodelEntity() {
    }

    public MyPanacheStaticMetamodelEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyPanacheEntity [id=" + id + ", name=" + name + "]";
    }

}
