package io.quarkus.mongodb.panache.transaction.interceptor;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.*;

import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.mongodb.panache.transaction.MongoTransactionConfiguration;
import io.quarkus.mongodb.panache.transaction.MongoTransactionException;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Uni;

@Interceptor
@Transactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 250) // needs to be after the narayana transactional interceptor
public class ReactiveMongoTransactionalInterceptor {
    public static final Object SESSION_KEY = new Object();

    @Inject
    ReactiveMongoClient client;

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager transactionManager;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        TransactionOptions.Builder txnOptions = TransactionOptions.builder();
        MongoTransactionConfiguration configuration = getTransactionConfiguration(ic);
        if (configuration != null) {
            if (!configuration.readConcern().equals("<default>")) {
                ReadConcern readConcern = new ReadConcern(ReadConcernLevel.fromString(configuration.readConcern()));
                txnOptions.readConcern(readConcern);
            }
            if (!configuration.writeConcern().equals("<default>")) {
                WriteConcern writeConcern = WriteConcern.valueOf(configuration.writeConcern());
                txnOptions.writeConcern(writeConcern);
            }
            if (!configuration.readPreference().equals("<default>")) {
                ReadPreference readPreference = ReadPreference.valueOf(configuration.readPreference());
                txnOptions.readPreference(readPreference);
            }
        }

        System.out.println("Openning Reactive MongoDB session");
        createSession(client, txnOptions.build());

        return ic.proceed();
    }

    private void createSession(ReactiveMongoClient client, TransactionOptions options) {
        // already have a transaction
        final ClientSession existing = (ClientSession) tsr.getResource(SESSION_KEY);
        if (existing != null) {
            return;
        }

        ClientSession clientSession = client.startSession().await().indefinitely();//FIXME we block here
        clientSession.startTransaction(options);

        // creates a new one
        tsr.putResource(SESSION_KEY, clientSession);
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int i) {
                try {
                    if (transactionManager.getStatus() == Status.STATUS_ROLLEDBACK) {
                        System.out.println("!!! Reactive MongoDB session rollback");
                        try {
                            Uni.createFrom().publisher(clientSession.abortTransaction()).await().indefinitely();//FIXME we block here
                        } finally {
                            clientSession.close();
                        }
                    } else {
                        System.out.println("!!! Reactive MongoDB session commit");
                        try {
                            Uni.createFrom().publisher(clientSession.commitTransaction()).await().indefinitely();//FIXME we block here
                        } finally {
                            clientSession.close();
                        }
                    }
                } catch (SystemException e) {
                    throw new MongoTransactionException(e);
                }
            }
        });
    }

    private MongoTransactionConfiguration getTransactionConfiguration(InvocationContext ic) {
        MongoTransactionConfiguration configuration = ic.getMethod().getAnnotation(MongoTransactionConfiguration.class);
        if (configuration == null) {
            return ic.getTarget().getClass().getAnnotation(MongoTransactionConfiguration.class);
        }

        return configuration;
    }
}
