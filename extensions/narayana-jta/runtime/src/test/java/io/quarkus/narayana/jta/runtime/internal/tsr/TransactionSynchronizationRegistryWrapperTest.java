package io.quarkus.narayana.jta.runtime.internal.tsr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;

import org.junit.jupiter.api.Test;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

public class TransactionSynchronizationRegistryWrapperTest {

    /**
     * The first registration in a transaction must only synchronize with other registrations in the same transaction,
     * not with registrations happening in unrelated transactions.
     */
    @Test
    void firstRegistrationDoesNotContendWithOtherTransactions() throws Exception {
        TransactionSynchronizationRegistryWrapper wrapper = new TransactionSynchronizationRegistryWrapper(
                new TransactionSynchronizationRegistryImple());
        TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

        // simulate another transaction being in the middle of its first registration by holding the
        // lock that used to be shared by all transactions
        Field keyField = TransactionSynchronizationRegistryWrapper.class.getDeclaredField("key");
        keyField.setAccessible(true);
        Object sharedKey = keyField.get(wrapper);

        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            synchronized (sharedKey) {
                held.countDown();
                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        holder.start();
        assertTrue(held.await(5, TimeUnit.SECONDS));

        CountDownLatch beforeCompletion = new CountDownLatch(1);
        try {
            CompletableFuture<Void> registration = CompletableFuture.runAsync(() -> {
                try {
                    tm.begin();
                    wrapper.registerInterposedSynchronization(new Synchronization() {
                        @Override
                        public void beforeCompletion() {
                            beforeCompletion.countDown();
                        }

                        @Override
                        public void afterCompletion(int status) {
                        }
                    });
                    tm.commit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            // before the fix this blocks until the holder releases the shared lock
            registration.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            holder.join();
        }
        assertTrue(beforeCompletion.await(0, TimeUnit.SECONDS));
    }
}
