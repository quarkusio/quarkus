package io.quarkus.data.hibernate.deployment.test.transactional;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

import io.quarkus.arc.Arc;
import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class TransactionalTestEntity extends ManagedEntity {

    public String name;

    @Repository
    public interface AnnotatedRepo {
        @Transactional
        @Find
        List<TransactionalTestEntity> findByName(String name);

        @Transactional
        default void checkTransactionActive() {
            TransactionalTestEntity.assertTransactionActive();
        }
    }

    public interface UnannotatedRepo {
        @Transactional
        @Find
        List<TransactionalTestEntity> findByName(String name);

        @Transactional
        default void checkTransactionActive() {
            TransactionalTestEntity.assertTransactionActive();
        }
    }

    public interface ManagedRepo extends ManagedRepository<TransactionalTestEntity> {
        @Transactional
        @Find
        List<TransactionalTestEntity> findByName(String name);

        @Transactional
        default void checkTransactionActive() {
            TransactionalTestEntity.assertTransactionActive();
        }
    }

    static void assertTransactionActive() {
        try {
            TransactionManager tm = Arc.container().select(TransactionManager.class).get();
            jakarta.transaction.Transaction tx = tm.getTransaction();
            if (tx == null || tx.getStatus() != Status.STATUS_ACTIVE) {
                throw new IllegalStateException("No active transaction");
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
