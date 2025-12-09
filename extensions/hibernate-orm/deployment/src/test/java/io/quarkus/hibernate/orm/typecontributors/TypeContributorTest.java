package io.quarkus.hibernate.orm.typecontributors;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TypeContributorTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(MyEntity.class)
                    .addClass(BooleanYesNoType.class)
                    .addClass(CustomTypeContributor.class));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void testCustomType() {
        MyEntity entity = new MyEntity();
        entity.active = true;

        entityManager.persist(entity);
        entityManager.flush();
        entityManager.clear();

        MyEntity savedEntity = entityManager.find(MyEntity.class, entity.getId());

        assertThat(savedEntity.active).isTrue();

        String savedValue = (String) entityManager.createNativeQuery("SELECT active FROM MyEntity WHERE id = " + entity.getId())
                .getSingleResult();
        assertThat(savedValue).isEqualTo("Y");
    }
}
