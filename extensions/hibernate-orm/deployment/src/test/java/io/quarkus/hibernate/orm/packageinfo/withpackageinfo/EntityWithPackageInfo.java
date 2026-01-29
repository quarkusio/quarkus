package io.quarkus.hibernate.orm.packageinfo.withpackageinfo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithPackageInfo {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public EntityWithPackageInfo() {
    }

    public EntityWithPackageInfo(String name) {
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
