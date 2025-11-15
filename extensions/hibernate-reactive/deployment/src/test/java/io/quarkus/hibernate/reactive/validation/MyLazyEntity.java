package io.quarkus.hibernate.reactive.validation;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;

@Table(name = "my_lazy_entity_table")
@Entity
public class MyLazyEntity {
    private long id;

    private String name;

    private Set<@Valid MyLazyChildEntity> children;

    public MyLazyEntity() {
    }

    public MyLazyEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    public Set<MyLazyChildEntity> getChildren() {
        return children;
    }

    public void setChildren(Set<MyLazyChildEntity> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "MyLazyEntity:" + name;
    }
}
