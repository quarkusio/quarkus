package io.quarkus.hibernate.orm.enums;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class EnumPersistenceTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntityWithEnum.class)
                    .addClass(Status.class))
            .withConfigurationResource("application.properties");

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void enumFieldPersistedAndRetrieved() {
        MyEntityWithEnum entity = new MyEntityWithEnum("Gizmo", Status.LIVING);
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        MyEntityWithEnum retrieved = entityManager.find(MyEntityWithEnum.class, entity.getId());
        assertThat(retrieved.getStatus()).isEqualTo(Status.LIVING);
        assertThat(retrieved.getName()).isEqualTo("Gizmo");
    }
}
