package io.quarkus.hibernate.reactive.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiMutinySessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Mutiny.Session session;

    @Test
    @ActivateRequestContext
    public void test() {
        DefaultEntity entity = new DefaultEntity("default");

        DefaultEntity retrievedEntity = session.persist(entity)
                .chain(() -> session.flush())
                .invoke(() -> session.clear())
                .chain(() -> session.find(DefaultEntity.class, entity.getId()))
                .await().indefinitely();

        assertThat(retrievedEntity)
                .isNotSameAs(entity)
                .returns(entity.getName(), DefaultEntity::getName);
    }

}
