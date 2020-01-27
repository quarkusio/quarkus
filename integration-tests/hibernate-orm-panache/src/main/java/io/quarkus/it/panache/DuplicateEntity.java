package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class DuplicateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public static DuplicateEntity findById(Object id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = (Integer) id;
        return duplicate;
    }

    @Override
    public void persist() {
        // Do nothing
    }
}
