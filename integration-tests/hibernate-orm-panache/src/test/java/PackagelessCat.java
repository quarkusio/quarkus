import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PackagelessCat extends PanacheEntity {

    public PackagelessCat() {
    }
}
