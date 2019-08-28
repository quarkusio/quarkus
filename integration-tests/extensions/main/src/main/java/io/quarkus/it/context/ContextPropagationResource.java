package io.quarkus.it.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.wildfly.common.Assert;

import io.quarkus.arc.Arc;

@Path("context-propagation")
public class ContextPropagationResource {

    @Inject
    RequestBean doNotRemove;
    @Inject
    ManagedExecutor allExec;
    @Inject
    TransactionManager transactionManager;

    @Transactional
    @GET
    public CompletionStage<String> test(@Context UriInfo uriInfo) throws SystemException {
        CompletableFuture<String> ret = allExec.completedFuture("OK");
        // Transaction
        Transaction t1 = transactionManager.getTransaction();
        Assert.assertTrue(t1 != null);
        Assert.assertTrue(t1.getStatus() == Status.STATUS_ACTIVE);
        // ArC
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean rb1 = Arc.container().instance(RequestBean.class).get();
        Assert.assertTrue(rb1 != null);
        String rbValue = rb1.callMe();

        return ret.thenApplyAsync(text -> {
            // RESTEasy
            uriInfo.getAbsolutePath();
            // ArC
            RequestBean rb2 = Arc.container().instance(RequestBean.class).get();
            Assert.assertTrue(rb2 != null);
            Assert.assertTrue(rb2.callMe().contentEquals(rbValue));
            // Transaction
            Transaction t2;
            try {
                t2 = transactionManager.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            Assert.assertTrue(t1 == t2);
            try {
                Assert.assertTrue(t2.getStatus() == Status.STATUS_ACTIVE);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            return text;
        });
    }
}
