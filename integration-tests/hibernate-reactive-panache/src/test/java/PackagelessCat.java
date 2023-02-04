import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class PackagelessCat extends PanacheEntity {

    public PackagelessCat() {
    }
}
