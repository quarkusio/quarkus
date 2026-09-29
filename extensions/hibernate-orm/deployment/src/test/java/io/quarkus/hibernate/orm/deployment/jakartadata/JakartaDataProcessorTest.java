package io.quarkus.hibernate.orm.deployment.jakartadata;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;

class JakartaDataProcessorTest {

    @Test
    void registersAnnotatedAndUnannotatedRepositoryInterfacesOnceInTheirPersistenceUnits() throws IOException {
        var indexer = new Indexer();
        for (Class<?> type : List.of(ExplicitOnly.class, AnnotatedWithQuery.class, Unannotated.class,
                WithHql.class, WithSql.class, ParentWithQuery.class, ChildOfParent.class, EntityWithNestedRepository.class,
                EntityWithNestedRepository.NestedRepository.class, NotARepository.class)) {
            indexer.indexClass(type);
        }
        var index = indexer.complete();
        List<AdditionalJpaModelBuildItem> contributions = new ArrayList<>();

        new JakartaDataProcessor().addJakartaDataRepositoriesToJpaModel(
                new CombinedIndexBuildItem(index, index), contributions::add);

        // Collecting into a map also fails if a repository is contributed twice.
        var persistenceUnitsByClass = contributions.stream().collect(Collectors.toMap(
                AdditionalJpaModelBuildItem::getClassName, AdditionalJpaModelBuildItem::getPersistenceUnits));
        assertThat(persistenceUnitsByClass)
                .hasSize(8)
                .containsEntry(ExplicitOnly.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(AnnotatedWithQuery.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(Unannotated.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(WithHql.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(WithSql.class.getName(), Set.of(DEFAULT_PERSISTENCE_UNIT_NAME))
                .containsEntry(ParentWithQuery.class.getName(), Set.of("other"))
                .containsEntry(ChildOfParent.class.getName(), Set.of("other"))
                .containsEntry(EntityWithNestedRepository.NestedRepository.class.getName(), Set.of())
                .doesNotContainKey(NotARepository.class.getName());
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
