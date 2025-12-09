package io.quarkus.hibernate.orm.typecontributors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;

@Entity
public class MyEntity {
    @Id
    private long id;

    @Type(value = BooleanYesNoType.class)
    public Boolean active;

    public MyEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
