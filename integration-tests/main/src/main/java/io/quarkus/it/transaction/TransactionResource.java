package io.quarkus.it.transaction;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/txn/txendpoint")
public class TransactionResource {

    @Inject
    UserTransaction userTransaction;

    @Inject
    TransactionSynchronizationRegistry trs;

    @GET
    public boolean tryTxn() throws Exception {
        final AtomicBoolean res = new AtomicBoolean();
        userTransaction.begin();
        trs.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                res.set(true);
            }

            @Override
            public void afterCompletion(int status) {

            }
        });
        userTransaction.commit();

        return res.get();
    }

}
