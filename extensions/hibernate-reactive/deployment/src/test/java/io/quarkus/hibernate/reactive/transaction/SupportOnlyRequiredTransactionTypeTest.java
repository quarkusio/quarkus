package io.quarkus.hibernate.reactive.transaction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.transaction.TransactionalInterceptorMandatory;
import io.quarkus.reactive.transaction.TransactionalInterceptorNever;
import io.quarkus.reactive.transaction.TransactionalInterceptorNotSupported;
import io.quarkus.reactive.transaction.TransactionalInterceptorRequiresNew;
import io.quarkus.reactive.transaction.TransactionalInterceptorSupports;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;

public class SupportOnlyRequiredTransactionTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addDefaultPackage()
                    .addClasses(
                            TransactionalInterceptorNever.class,
                            TransactionalInterceptorSupports.class,
                            TransactionalInterceptorRequiresNew.class,
                            TransactionalInterceptorMandatory.class,
                            TransactionalInterceptorNotSupported.class)

            );

    private static final String ERROR_MESSAGE = "@Transactional on Reactive methods supports only Transactional.TxType.REQUIRED";

    @Test
    @RunOnVertxContext
    public void testMandatory() {
        assertThatThrownBy(() -> mandatory())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public Uni<?> mandatory() {
        return Uni.createFrom().item("mandatory");
    }

    @Test
    @RunOnVertxContext
    public void testNever() {
        assertThatThrownBy(() -> never())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Uni<?> never() {
        return Uni.createFrom().item("never");
    }

    @Test
    @RunOnVertxContext
    public void testNotSupported() {
        assertThatThrownBy(() -> notSupported())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    @RunOnVertxContext
    public Uni<?> notSupported() {
        return Uni.createFrom().item("not_supported");
    }

    @Test
    @RunOnVertxContext
    public void testRequiresNew() {
        assertThatThrownBy(() -> requiresNew())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Uni<?> requiresNew() {
        return Uni.createFrom().item("requiresNew");
    }

    @Test
    @RunOnVertxContext
    public void testSupports() {
        assertThatThrownBy(() -> supports())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(ERROR_MESSAGE);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Uni<String> supports() {
        return Uni.createFrom().item("supports");
    }
}
