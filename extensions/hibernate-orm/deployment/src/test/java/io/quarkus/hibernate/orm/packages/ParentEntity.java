package io.quarkus.hibernate.orm.packages;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.hibernate.annotations.Any;

@Entity
public class ParentEntity {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @Any(metaDef = "childrenAnyMetaDef", metaColumn = @Column(name = "child_type"))
    @JoinColumn(name = "child_id")
    private Object child;

    public ParentEntity() {
    }

    public ParentEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ParentEntity:" + name;
    }

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

    public Object getChild() {
        return child;
    }

    public void setChild(Object child) {
        this.child = child;
    }
}
