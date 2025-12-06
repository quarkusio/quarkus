package io.quarkus.hibernate.orm.functioncontributors;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionContributorTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(CustomFunctionContributor.class));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        MyEntity entity = new MyEntity();
        entity.setName("some_name");
        entityManager.persist(entity);

        assertThat(entityManager.createQuery("select addHardcodedSuffix(e.name) from MyEntity e", String.class)
                .getSingleResult())
                .isEqualTo("some_name_some_suffix");
    }
}
