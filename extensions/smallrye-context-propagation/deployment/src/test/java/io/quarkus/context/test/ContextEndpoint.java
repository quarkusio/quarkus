package io.quarkus.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Assertions;
import org.wildfly.common.Assert;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.Panache;

@Path("/context")
@Produces(MediaType.TEXT_PLAIN)
public class ContextEndpoint {

    @Inject
    RequestBean doNotRemoveMe;
    @Inject
    ManagedExecutor all;
    @Inject
    ThreadContext allTc;

    @GET
    @Path("/resteasy")
    public CompletionStage<String> resteasyTest(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = all.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            uriInfo.getAbsolutePath();
            return text;
        });
    }

    @GET
    @Path("/thread-context")
    public CompletionStage<String> threadContextTest(@Context UriInfo uriInfo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return ret.thenApplyAsync(text -> {
            uriInfo.getAbsolutePath();
            return text;
        }, executor);
    }

    @GET
    @Path("/arc")
    public CompletionStage<String> arcTest() {
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = all.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            Assertions.assertEquals(previousValue, instance2.callMe());
            return text;
        });
    }

    @GET
    @Path("/noarc")
    public CompletionStage<String> noarcTest() {
        ManagedExecutor me = ManagedExecutor.builder().cleared(ThreadContext.CDI).build();
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = me.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            Assertions.assertNotEquals(previousValue, instance2.callMe());
            return text;
        });
    }

    @Inject
    TransactionalBean txBean;

    @Transactional
    @GET
    @Path("/transaction")
    public CompletionStage<String> transactionTest() throws SystemException {
        CompletableFuture<String> ret = all.completedFuture("OK");

        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        return ret.thenApplyAsync(text -> {
            Assertions.assertEquals(1, ContextEntity.count());
            Transaction t2;
            try {
                t2 = Panache.getTransactionManager().getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            Assertions.assertEquals(t1, t2);
            return text;
        });
    }

    @Transactional
    @GET
    @Path("/transaction2")
    public CompletionStage<String> transactionTest2() throws SystemException {
        CompletableFuture<String> ret = all.completedFuture("OK");

        // check that the first transaction was committed
        Assertions.assertEquals(1, ContextEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, ContextEntity.deleteAll());

        return ret.thenApplyAsync(text -> {
            throw new WebApplicationException(Response.status(Status.CONFLICT).build());
        });
    }

    @Transactional
    @GET
    @Path("/transaction3")
    public CompletionStage<String> transactionTest3() throws SystemException {
        CompletableFuture<String> ret = all.failedFuture(new WebApplicationException(Response.status(Status.CONFLICT).build()));

        // check that the second transaction was not committed
        Assertions.assertEquals(1, ContextEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, ContextEntity.deleteAll());

        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction4")
    public String transactionTest4() throws SystemException {
        // check that the third transaction was not committed
        Assertions.assertEquals(1, ContextEntity.count());
        // now delete our entity
        Assertions.assertEquals(1, ContextEntity.deleteAll());

        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction-new")
    public CompletionStage<String> transactionNewTest() throws SystemException {
        CompletableFuture<String> ret = all.completedFuture("OK");

        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        txBean.doInTx();

        // We should see the transaction already committed even if we're async
        Assertions.assertEquals(1, ContextEntity.deleteAll());
        return ret;
    }
}
