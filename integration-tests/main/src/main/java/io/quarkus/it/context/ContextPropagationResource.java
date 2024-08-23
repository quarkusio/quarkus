package io.quarkus.it.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ThreadContext;
import org.wildfly.common.Assert;

import io.quarkus.arc.Arc;
import io.smallrye.context.SmallRyeManagedExecutor;

@Path("context-propagation")
public class ContextPropagationResource {

    @Inject
    RequestBean doNotRemove;
    @Inject
    SmallRyeManagedExecutor smallRyeAllExec;
    @Inject
    SmallRyeManagedExecutor allExec;
    @Inject
    ThreadContext context;
    @Inject
    ThreadContext smallRyeContext;
    @Inject
    TransactionManager transactionManager;

    @Path("managed-executor/created")
    @Transactional
    @GET
    public CompletionStage<String> testManagedExecutorCreated(@Context UriInfo uriInfo) throws SystemException {
        return testCompletionStage(allExec.completedFuture("OK"), uriInfo);
    }

    @Path("managed-executor/obtained")
    @Transactional
    @GET
    public CompletionStage<String> testManagedExecutorObtained(@Context UriInfo uriInfo) throws SystemException {
        // make sure we can also do that with CF we obtained from other sources, via ManagedExecutor
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("OK");
        return testCompletionStage(allExec.copy(completedFuture), uriInfo);
    }

    @Path("thread-context")
    @Transactional
    @GET
    public CompletionStage<String> testThreadContext(@Context UriInfo uriInfo) throws SystemException {
        // make sure we can also do that with CF we obtained from other sources, via ThreadContext
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("OK");
        return testCompletionStage(context.withContextCapture(completedFuture), uriInfo);
    }

    private CompletionStage<String> testCompletionStage(CompletionStage<String> stage, UriInfo uriInfo) throws SystemException {
        // Transaction
        Transaction t1 = transactionManager.getTransaction();
        Assert.assertTrue(t1 != null);
        Assert.assertTrue(t1.getStatus() == Status.STATUS_ACTIVE);
        // ArC
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean rb1 = Arc.container().instance(RequestBean.class).get();
        Assert.assertTrue(rb1 != null);
        String rbValue = rb1.callMe();

        return stage.thenApplyAsync(text -> {
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
