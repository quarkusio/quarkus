package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.PersistenceAgent;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SinglePersistenceUnitResourceInjectionStatelessSessionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @PersistenceAgent
    StatelessSession agent;

    @Test
    @Transactional
    public void test() {
        DefaultEntity entity = new DefaultEntity("gsmet");
        agent.insert(entity);

        DefaultEntity savedEntity = agent.createQuery("from DefaultEntity where id = :id", DefaultEntity.class)
                .setParameter("id", entity.getId())
                .getSingleResult();
        assertThat(savedEntity.getName()).isEqualTo(entity.getName());
    }

}
