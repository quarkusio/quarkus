package io.quarkus.data.hibernate.deployment.test.transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.lang.reflect.Method;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

@ActivateRequestContext
class TransactionalRepositoryTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addPackage(TransactionalTestEntity.class.getPackage()));

    @Test
    void testTransactionalCopiedToAnnotatedRepoImpl() throws Exception {
        assertTransactionalOnFindMethod(TransactionalTestEntity_.AnnotatedRepo_.class);
    }

    @Test
    void testTransactionalCopiedToUnannotatedRepoImpl() throws Exception {
        assertTransactionalOnFindMethod(TransactionalTestEntity_.UnannotatedRepo_.class);
    }

    @Test
    void testTransactionalCopiedToManagedRepoImpl() throws Exception {
        assertTransactionalOnFindMethod(TransactionalTestEntity_.ManagedRepo_.class);
    }

    @Test
    void testAnnotatedRepoDefaultMethodTransaction() {
        assertThatNoException().isThrownBy(
                () -> Arc.container().select(TransactionalTestEntity.AnnotatedRepo.class).get()
                        .checkTransactionActive());
    }

    @Test
    void testUnannotatedRepoDefaultMethodTransaction() {
        assertThatNoException().isThrownBy(
                () -> Arc.container().select(TransactionalTestEntity.UnannotatedRepo.class).get()
                        .checkTransactionActive());
    }

    @Test
    void testManagedRepoDefaultMethodTransaction() {
        assertThatNoException().isThrownBy(
                () -> Arc.container().select(TransactionalTestEntity.ManagedRepo.class).get()
                        .checkTransactionActive());
    }

    private void assertTransactionalOnFindMethod(Class<?> implClass) throws Exception {
        Method method = implClass.getMethod("findByName", String.class);
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as("@Transactional should be present on %s.findByName", implClass.getSimpleName())
                .isTrue();
    }
}
