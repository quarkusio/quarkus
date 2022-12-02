package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

@Entity
public class DuplicateEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public static <T extends PanacheEntityBase> T findById(Object id) {
        DuplicateEntity duplicate = new DuplicateEntity();
        duplicate.id = (Integer) id;
        return (T) duplicate;
    }

    @Override
    public void persist() {
        // Do nothing
    }

    public static int update(String query, Parameters params) {
        return 0;
    }
}
