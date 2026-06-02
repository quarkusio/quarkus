package io.quarkus.hibernate.panache.deployment.test.processor;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;

@Entity
public class EntityWithBadRepoNames extends PanacheEntity {
    // These repos lie, they're just here to validate that we don't generate clashing accessors
    public interface ManagedBlocking {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface StatelessBlocking {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface ManagedReactive {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface StatelessReactive {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    // This forces a rename of the generated repo
    public interface PanacheStatelessReactiveRepository {
        @Find
        List<EntityWithBadRepoNames> all();
    }
}
