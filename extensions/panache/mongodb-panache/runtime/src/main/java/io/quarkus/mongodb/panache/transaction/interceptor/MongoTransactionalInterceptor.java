package io.quarkus.mongodb.panache.transaction.interceptor;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.TransactionBody;

import io.quarkus.mongodb.panache.transaction.MongoTransactionException;
import io.quarkus.mongodb.panache.transaction.Transactional;

@Interceptor
@Transactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class MongoTransactionalInterceptor {
    @Inject
    private MongoClient client;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        // if we are in a sub-method call we just proceed with the invocation
        // note that nested transactions are not supported
        if (TransactionManager.activeTransaction()) {
            return ic.proceed();
        }

        Transactional transactional = getTransactional(ic);
        TransactionOptions.Builder txnOptions = TransactionOptions.builder();
        if (!transactional.readConcern().equals("<default>")) {
            ReadConcern readConcern = new ReadConcern(ReadConcernLevel.fromString(transactional.readConcern()));
            txnOptions.readConcern(readConcern);
        }
        if (!transactional.writeConcern().equals("<default>")) {
            WriteConcern writeConcern = WriteConcern.valueOf(transactional.writeConcern());
            txnOptions.writeConcern(writeConcern);
        }
        if (!transactional.readPreference().equals("<default>")) {
            ReadPreference readPreference = ReadPreference.valueOf(transactional.readPreference());
            txnOptions.readPreference(readPreference);
        }

        TransactionBody txnBody = new TransactionBody() {
            public Object execute() {
                try {
                    return ic.proceed();
                } catch (Exception e) {
                    throw new MongoTransactionException(e);
                }
            }
        };

        try (ClientSession clientSession = client.startSession()) {
            Transaction transaction = new Transaction(clientSession);
            TransactionManager.setTransaction(transaction);
            return clientSession.withTransaction(txnBody, txnOptions.build());
        } finally {
            TransactionManager.clear();
        }
    }

    private Transactional getTransactional(InvocationContext ic) {
        Transactional configuration = ic.getMethod().getAnnotation(Transactional.class);
        if (configuration == null) {
            return ic.getTarget().getClass().getAnnotation(Transactional.class);
        }

        return configuration;
    }
}
