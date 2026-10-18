package io.quarkus.it.hibernate.search.orm.elasticsearch.management;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class ManagementTestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "managementSeq")
    private Long id;

    @FullTextField
    private String name;

    public ManagementTestEntity() {
    }

    public ManagementTestEntity(String name) {
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
