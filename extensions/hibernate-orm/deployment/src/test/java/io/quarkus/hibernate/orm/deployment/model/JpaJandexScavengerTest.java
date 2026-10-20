package io.quarkus.hibernate.orm.deployment.model;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.deployment.model.JpaJandexScavenger.Collector;

class JpaJandexScavengerTest {

    @Test
    void enlistsRepositoryInterfacesAndTheirExplicitPersistenceUnits() throws IOException {
        Index index = Index.of(ExplicitOnly.class, AnnotatedWithQuery.class, Unannotated.class, WithHql.class,
                WithSql.class, ParentWithQuery.class, ChildOfParent.class, EntityWithNestedRepository.class,
                EntityWithNestedRepository.NestedRepository.class, NotARepository.class);
        Collector collector = new Collector();

        JpaJandexScavenger.enlistJakartaDataRepositories(collector, index);

        // Every repository interface is registered as a model type exactly once; NotARepository is a class, not a repo.
        assertThat(collector.modelTypes).contains(
                ExplicitOnly.class.getName(),
                AnnotatedWithQuery.class.getName(),
                Unannotated.class.getName(),
                WithHql.class.getName(),
                WithSql.class.getName(),
                ParentWithQuery.class.getName(),
                ChildOfParent.class.getName(),
                EntityWithNestedRepository.NestedRepository.class.getName());
        assertThat(collector.modelTypes).doesNotContain(NotARepository.class.getName());

        assertThat(collector.repositoryPersistenceUnits)
                .containsEntry(ExplicitOnly.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(AnnotatedWithQuery.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                // Unannotated top-level repositories follow package-based rules, so they carry no explicit assignment.
                .doesNotContainKey(Unannotated.class.getName())
                .doesNotContainKey(WithHql.class.getName())
                .doesNotContainKey(WithSql.class.getName())
                // An unannotated superinterface inherits the PU of its annotated subinterface.
                .containsEntry(ParentWithQuery.class.getName(), Set.of("other"))
                .containsEntry(ChildOfParent.class.getName(), Set.of("other"))
                // A nested repository carries an empty marker; its PU is resolved from its enclosing entity later.
                .containsEntry(EntityWithNestedRepository.NestedRepository.class.getName(), Set.of());
    }

    @Repository
    interface ExplicitOnly {
    }

    @Repository
    interface AnnotatedWithQuery {
        @Query("select 1")
        int count();
    }

    interface Unannotated {
        @Query("select 1")
        int count();
    }

    interface WithHql {
        @HQL("select 1")
        int count();
    }

    interface WithSql {
        @SQL("select 1")
        int count();
    }

    interface ParentWithQuery {
        @Query("select 1")
        int count();
    }

    @Repository(dataStore = "other")
    interface ChildOfParent extends ParentWithQuery {
    }

    @Entity
    static class EntityWithNestedRepository {
        interface NestedRepository {
            @Query("select 1")
            int count();
        }
    }

    static class NotARepository {
        @Query("select 1")
        int count() {
            return 1;
        }
    }
}
