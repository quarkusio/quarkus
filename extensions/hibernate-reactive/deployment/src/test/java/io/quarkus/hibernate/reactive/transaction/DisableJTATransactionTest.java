package io.quarkus.hibernate.reactive.transaction;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class DisableJTATransactionTest {

    @Inject
    TransactionManager transactionManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-narayana-jta", Version.getVersion())));

    @Test
    @RunOnVertxContext
    public void doNotCreateTransactionIfMethodIsAUni(UniAsserter asserter) throws SystemException {
        asserter.assertThat(() -> transactionUniMethod(), h -> {
            try {
                assertNull(transactionManager.getTransaction());
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Transactional(Transactional.TxType.REQUIRED)
    Uni<String> transactionUniMethod() {
        return Uni.createFrom().item("transactionalUniMethod");
    }

}
