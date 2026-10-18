package io.quarkus.hibernate.orm.packageinfo.withoutpackageinfo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityWithoutPackageInfo {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public EntityWithoutPackageInfo() {
    }

    public EntityWithoutPackageInfo(String name) {
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
