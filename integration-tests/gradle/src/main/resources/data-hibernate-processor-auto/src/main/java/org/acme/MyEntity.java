package org.acme;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import jakarta.persistence.Entity;
import java.util.List;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

@Entity
public class MyEntity extends ManagedEntity {

    public String name;
    public int amount;

    // Repository interface using @Find annotation (requires quarkus-data-processor)
    public interface Queries extends ManagedRepository<MyEntity> {
        @Find
        List<MyEntity> findByName(String name);

        @HQL("where amount > :minAmount")
        List<MyEntity> findByAmountGreaterThan(int minAmount);
    }
}
