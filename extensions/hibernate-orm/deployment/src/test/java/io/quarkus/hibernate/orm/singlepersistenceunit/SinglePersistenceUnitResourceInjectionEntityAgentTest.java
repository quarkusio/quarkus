package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.PersistenceAgent;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SinglePersistenceUnitResourceInjectionEntityAgentTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @PersistenceAgent
    EntityAgent entityAgent;

    @PersistenceAgent
    StatelessSession statelessSession;

    @Test
    @Transactional
    public void testEntityAgent() {
        assertThat(entityAgent).isNotNull();
        DefaultEntity entity = new DefaultEntity("entityAgent");
        entityAgent.insert(entity);

        assertThat(entityAgent.find(DefaultEntity.class, entity.getId()).getName()).isEqualTo(entity.getName());
    }

    @Test
    @Transactional
    public void testStatelessSession() {
        assertThat(statelessSession).isNotNull();
        DefaultEntity entity = new DefaultEntity("statelessSession");
        statelessSession.insert(entity);

        assertThat(statelessSession.get(DefaultEntity.class, entity.getId()).getName()).isEqualTo(entity.getName());
    }
}
