package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorBootstrapTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator"),
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator-deployment")))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:accessor-bootstrap")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "reflection-free");

    @Inject
    EntityManager entityManager;

    @Test
    void persistAndLoadWithoutSearchOrValidator() {
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = new MyEntity();
            entity.id = 1L;
            entity.name = "initial";
            entityManager.persist(entity);
            entityManager.flush();
            entityManager.clear();
            assertThat(entityManager.find(MyEntity.class, 1L).name).isEqualTo("initial");
            entityManager.find(MyEntity.class, 1L).name = "updated";
        });
        QuarkusTransaction.requiringNew()
                .run(() -> assertThat(entityManager.find(MyEntity.class, 1L).name).isEqualTo("updated"));
    }

    @Entity
    public static class MyEntity {
        @Id
        public Long id;
        public String name;
    }
}
