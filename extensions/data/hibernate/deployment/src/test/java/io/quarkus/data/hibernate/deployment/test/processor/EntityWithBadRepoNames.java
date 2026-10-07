package io.quarkus.data.hibernate.deployment.test.processor;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;

@Entity
public class EntityWithBadRepoNames extends ManagedEntity {
    // These repos lie, they're just here to validate that we don't generate clashing accessors
    public interface Managed {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface Record {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface ManagedReactive {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    public interface RecordReactive {
        @Find
        List<EntityWithBadRepoNames> all();
    }

    // This forces a rename of the generated repo
    public interface QuarkusDataRecordReactiveRepository {
        @Find
        List<EntityWithBadRepoNames> all();
    }
}
