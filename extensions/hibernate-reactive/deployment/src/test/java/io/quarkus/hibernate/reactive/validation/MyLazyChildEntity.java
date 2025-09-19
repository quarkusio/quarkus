package io.quarkus.hibernate.reactive.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Table(name = "my_lazy_entity_child_table")
@Entity
public class MyLazyChildEntity {
    public interface SomeGroup {
    }

    public static final String ENTITY_NAME_TOO_LONG = "entity name too long";
    private long id;

    // Use a non-default validation group so that validation won't trigger an exception on persist:
    @Size(max = 5, message = ENTITY_NAME_TOO_LONG, groups = { SomeGroup.class })
    private String name;

    private MyLazyEntity parent;

    public MyLazyChildEntity() {
    }

    public MyLazyChildEntity(String name) {
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

    @ManyToOne
    public MyLazyEntity getParent() {
        return parent;
    }

    public void setParent(MyLazyEntity parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "MyLazyChildEntity:" + name;
    }
}
