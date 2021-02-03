package io.quarkus.hibernate.reactive.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.hibernate.reactive.stage.Stage;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiStageSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Stage.Session session;

    @Test
    @ActivateRequestContext
    public void test() {
        DefaultEntity entity = new DefaultEntity("default");

        DefaultEntity retrievedEntity = session.persist(entity)
                .thenCompose($ -> session.flush())
                .thenApply($ -> session.clear())
                .thenCompose($ -> session.find(DefaultEntity.class, entity.getId()))
                .toCompletableFuture().join();

        assertThat(retrievedEntity)
                .isNotSameAs(entity)
                .returns(entity.getName(), DefaultEntity::getName);
    }

}
