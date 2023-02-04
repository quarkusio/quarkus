package io.quarkus.it.jpa.orderbyfragment;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Parent {

    @Id
    private Long id;

    @OneToMany(mappedBy = "parent")
    @OrderBy("forename")
    private List<Child> children;

    public List<Child> getChildren() {
        return children;
    }

    public void setChildren(List<Child> children) {
        this.children = children;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
