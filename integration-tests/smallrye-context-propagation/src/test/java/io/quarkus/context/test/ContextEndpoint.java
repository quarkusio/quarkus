package io.quarkus.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.annotations.Stream.MODE;
import org.junit.jupiter.api.Assertions;
import org.reactivestreams.Publisher;
import org.wildfly.common.Assert;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.Panache;
import io.reactivex.Single;
import io.smallrye.mutiny.Multi;

@Path("/context")
@Produces(MediaType.TEXT_PLAIN)
public class ContextEndpoint {

    @Inject
    RequestBean doNotRemoveMe;
    @Inject
    ManagedExecutor all;
    @Inject
    ThreadContext allTc;
    @Inject
    HttpServletRequest servletRequest;

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
    @Path("/resteasy-tc")
    public CompletionStage<String> resteasyThreadContextTest(@Context UriInfo uriInfo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return ret.thenApplyAsync(text -> {
            uriInfo.getAbsolutePath();
            return text;
        }, executor);
    }

    @GET
    @Path("/servlet")
    public CompletionStage<String> servletTest(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = all.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            servletRequest.getContentType();
            return text;
        });
    }

    @GET
    @Path("/servlet-tc")
    public CompletionStage<String> servletThreadContextTest(@Context UriInfo uriInfo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return ret.thenApplyAsync(text -> {
            servletRequest.getContentType();
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
    @Path("/arc-tc")
    public CompletionStage<String> arcThreadContextTest() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            Assertions.assertEquals(previousValue, instance2.callMe());
            return text;
        }, executor);
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

    @GET
    @Path("/noarc-tc")
    public CompletionStage<String> noarcThreadContextTest() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ThreadContext tc = ThreadContext.builder().cleared(ThreadContext.CDI).build();
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = tc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
            Assertions.assertNotEquals(previousValue, instance2.callMe());
            return text;
        }, executor);
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
    @Path("/transaction-tc")
    public CompletionStage<String> transactionThreadContextTest() throws SystemException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

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
        }, executor);
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
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
        });
    }

    @Transactional
    @GET
    @Path("/transaction3")
    public CompletionStage<String> transactionTest3() throws SystemException {
        CompletableFuture<String> ret = all
                .failedFuture(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));

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

    @Transactional
    @GET
    @Path("/transaction-single")
    public Single<String> transactionSingle() throws SystemException {
        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);
        // our entity
        Assertions.assertEquals(1, ContextEntity.count());

        return txBean.doInTxSingle()
                // this makes sure we get executed in another scheduler
                .delay(100, TimeUnit.MILLISECONDS)
                .map(text -> {
                    // make sure we don't see the other transaction's entity
                    Transaction t2;
                    try {
                        t2 = Panache.getTransactionManager().getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    Assertions.assertEquals(t1, t2);
                    Assertions.assertEquals(Status.STATUS_ACTIVE, t2.getStatus());
                    return text;
                });
    }

    @Transactional
    @GET
    @Path("/transaction-single2")
    public Single<String> transactionSingle2() throws SystemException {
        Single<String> ret = Single.just("OK");
        // now delete both entities
        Assertions.assertEquals(2, ContextEntity.deleteAll());
        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction-publisher")
    @Stream(value = MODE.RAW)
    public Publisher<String> transactionPublisher() throws SystemException {
        ContextEntity entity = new ContextEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);
        // our entity
        Assertions.assertEquals(1, ContextEntity.count());

        return txBean.doInTxPublisher()
                // this makes sure we get executed in another scheduler
                .delay(100, TimeUnit.MILLISECONDS)
                .map(text -> {
                    // make sure we don't see the other transaction's entity
                    Transaction t2;
                    try {
                        t2 = Panache.getTransactionManager().getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    Assertions.assertEquals(t1, t2);
                    Assertions.assertEquals(Status.STATUS_ACTIVE, t2.getStatus());
                    return text;
                });
    }

    @Transactional
    @GET
    @Path("/transaction-publisher2")
    public Publisher<String> transactionPublisher2() {
        Publisher<String> ret = Multi.createFrom().item("OK");
        // now delete both entities
        Assertions.assertEquals(2, ContextEntity.deleteAll());
        return ret;
    }
}
