package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiCriteriaBuilderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    CriteriaBuilder criteriaBuilder;
    @Inject
    HibernateCriteriaBuilder hibernateCriteriaBuilder;

    @Test
    public void testCriteriaBuilder() {
        assertNotNull(criteriaBuilder);

        CriteriaQuery<DefaultEntity> equalQuery = criteriaBuilder.createQuery(DefaultEntity.class);
        Root<DefaultEntity> root = equalQuery.from(DefaultEntity.class);
        equalQuery.select(root)
                .where(criteriaBuilder.equal(root.get("name"), "test"));
        assertNotNull(equalQuery);
    }

    @Test
    public void testHibernateCriteriaBuilder() {
        assertNotNull(hibernateCriteriaBuilder);

        CriteriaQuery<DefaultEntity> equalQuery = hibernateCriteriaBuilder.createQuery(DefaultEntity.class);
        Root<DefaultEntity> root = equalQuery.from(DefaultEntity.class);
        equalQuery.select(root)
                .where(hibernateCriteriaBuilder.equal(root.get("name"), "test"));
        assertNotNull(equalQuery);
    }

}
