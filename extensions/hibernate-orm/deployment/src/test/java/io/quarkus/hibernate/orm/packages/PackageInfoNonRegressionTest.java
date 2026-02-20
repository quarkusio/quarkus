package io.quarkus.hibernate.orm.packages;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PackageInfoNonRegressionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addPackage(PackageInfoNonRegressionTest.class.getPackage())
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() > Level.WARNING.intValue())
            .assertLogRecords(logs -> assertThat(logs).noneMatch(
                    log -> log.getMessage().contains("ClassLoader QuarkusClassLoader")));

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    @Test
    @Transactional
    public void test() {

        ParentEntity parent1 = new ParentEntity("parent1");
        entityManager.persist(parent1);

        ParentEntity parent2 = new ParentEntity("parent2");
        entityManager.persist(parent2);

        List<ParentEntity> entities = entityManager.createQuery("from ParentEntity ", ParentEntity.class).getResultList();
        assertEquals(2, entities.size());

    }
}