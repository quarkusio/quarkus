package org.acme;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.persistence.Entity;
import java.util.List;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

@Entity
public class MyEntity extends PanacheEntity {

    public String name;
    public int amount;

    // Repository interface using @Find annotation (requires hibernate-processor)
    public interface Queries extends PanacheRepository<MyEntity> {
        @Find
        List<MyEntity> findByName(String name);

        @HQL("where amount > :minAmount")
        List<MyEntity> findByAmountGreaterThan(int minAmount);
    }
}
