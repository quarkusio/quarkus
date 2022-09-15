package io.quarkus.context.test.mutiny;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;
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
import io.quarkus.context.test.RequestBean;
import io.quarkus.hibernate.orm.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@Path("/mutiny-context")
@Produces(MediaType.TEXT_PLAIN)
public class MutinyContextEndpoint {

    @Inject
    RequestBean doNotRemoveMe;
    @Inject
    ManagedExecutor all;
    @Inject
    ThreadContext allTc;
    @Inject
    HttpServletRequest servletRequest;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    @GET
    @Path("/resteasy-uni-cs")
    public Uni<String> resteasyContextPropagationWithUniCreatedFromCSWithManagedExecutor(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = all.completedFuture("OK");
        return Uni.createFrom().completionStage(ret)
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().transform(s -> {
                    Assertions.assertNotNull(uriInfo.getAbsolutePath());
                    try {
                        Assertions.assertTrue(
                                uriInfo.getAbsolutePath().toURL().toExternalForm().contains("/resteasy-uni-cs"));
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                    return s;
                });
    }

    @GET
    @Path("/resteasy-uni")
    public Uni<String> resteasyContextPropagation(@Context UriInfo uriInfo) {
        return Uni.createFrom().item("OK")
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().transform(s -> {
                    Assertions.assertNotNull(uriInfo.getAbsolutePath());
                    try {
                        Assertions.assertTrue(
                                uriInfo.getAbsolutePath().toURL().toExternalForm().contains("/resteasy-uni"));
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                    return s;
                });
    }

    @GET
    @Path("/resteasy-tc-uni-cs")
    public Uni<String> resteasyThreadContextWithCS(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return Uni.createFrom().completionStage(ret)
                .emitOn(executor)
                .onItem().transform(s -> {
                    uriInfo.getAbsolutePath();
                    return s;
                });
    }

    @GET
    @Path("/servlet-uni")
    public Uni<String> servletContextPropagation(@Context UriInfo uriInfo) {
        return Uni.createFrom().item("OK")
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(text -> {
                    Assertions.assertNotNull(servletRequest.getContentType());
                    return text;
                })
                .onFailure().invoke(t -> System.out.println("Got failure " + t.getMessage()));
    }

    @GET
    @Path("/servlet-uni-cs")
    public Uni<String> servletContextPropagationWithUniCreatedFromCSWithManagedExecutor(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = all.completedFuture("OK");
        return Uni.createFrom().completionStage(() -> ret)
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(text -> {
                    Assertions.assertNotNull(servletRequest.getContentType());
                    return text;
                });
    }

    @GET
    @Path("/servlet-tc-uni-cs")
    public Uni<String> servletThreadContext(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return Uni.createFrom().completionStage(() -> ret)
                .emitOn(executor)
                .map(text -> {
                    Assertions.assertNotNull(servletRequest.getContentType());
                    return text;
                });
    }

    @GET
    @Path("/arc-uni")
    public Uni<String> arcContextPropagation() {
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        return Uni.createFrom().item("OK")
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(text -> {
                    RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
                    Assertions.assertEquals(previousValue, instance2.callMe());
                    return text;
                });
    }

    @GET
    @Path("/arc-uni-cs")
    public Uni<String> arcContextPropagationWithUniCreatedFromCSWithManagedExecutor() {
        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = all.completedFuture("OK");
        return Uni.createFrom().completionStage(() -> ret)
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(text -> {
                    RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
                    Assertions.assertEquals(previousValue, instance2.callMe());
                    return text;
                });
    }

    @GET
    @Path("/arc-tc-uni")
    public Uni<String> arcContextPropagationWithThreadContext() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Assert.assertTrue(Arc.container().instance(RequestBean.class).isAvailable());
        RequestBean instance = Arc.container().instance(RequestBean.class).get();
        String previousValue = instance.callMe();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        return Uni.createFrom().completionStage(() -> ret)
                .emitOn(executor)
                .map(text -> {
                    RequestBean instance2 = Arc.container().instance(RequestBean.class).get();
                    Assertions.assertEquals(previousValue, instance2.callMe());
                    return text;
                });
    }

    @Inject
    MutinyTransactionalBean txBean;

    @Transactional
    @GET
    @Path("/transaction-uni")
    public Uni<String> contextPropagationWithTxAndUni() throws SystemException {
        SomeEntity.deleteAll();
        Uni<String> ret = Uni.createFrom().item("OK");
        SomeEntity entity = new SomeEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        return ret
                .emitOn(executor)
                .map(text -> {
                    Assertions.assertEquals(1, SomeEntity.count());
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
    @Path("/transaction-uni-cs")
    public Uni<String> contextPropagationWithTxAndUniCreatedFromCS() throws SystemException {
        Uni<String> ret = Uni.createFrom().completionStage(all.completedFuture("OK"));
        SomeOtherEntity entity = new SomeOtherEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        return ret
                .emitOn(executor)
                .map(text -> {
                    Assertions.assertEquals(1, SomeOtherEntity.count());
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
    @Path("/transaction-tc-uni")
    public Uni<String> transactionPropagationWithThreadContextAndUniCreatedFromCS() throws SystemException {
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));
        SomeEntity entity = new SomeEntity();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        return Uni.createFrom().completionStage(ret)
                .emitOn(executor)
                .map(text -> {
                    Assertions.assertEquals(1, SomeEntity.count());
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
    @Path("/transaction2-uni")
    public Uni<String> transactionTest2() {
        Uni<String> ret = Uni.createFrom().item("OK");

        // check that the first transaction was committed
        Assertions.assertEquals(1, SomeEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, SomeEntity.deleteAll());

        return ret
                .emitOn(executor)
                .onItem().failWith(s -> new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
    }

    @Transactional
    @GET
    @Path("/transaction2-uni-cs")
    public Uni<String> transactionTest2WithUniCreatedFromCS() {
        Uni<String> ret = Uni.createFrom().completionStage(all.completedFuture("OK"));

        // check that the first transaction was committed
        Assertions.assertEquals(1, SomeOtherEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, SomeOtherEntity.deleteAll());

        return ret
                .emitOn(executor)
                .onItem().failWith(s -> new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
    }

    @Transactional
    @GET
    @Path("/transaction3-uni")
    public Uni<String> transactionTest3() {
        Uni<String> ret = Uni.createFrom()
                .failure(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));

        // check that the second transaction was not committed
        Assertions.assertEquals(1, SomeEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, SomeEntity.deleteAll());

        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction3-uni-cs")
    public Uni<String> transactionTest3WithUniCreatedFromCS() {
        CompletableFuture<String> future = all
                .failedFuture(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
        Uni<String> ret = Uni.createFrom().completionStage(future);

        // check that the second transaction was not committed
        Assertions.assertEquals(1, SomeOtherEntity.count());
        // now delete our entity, but throw an exception to rollback
        Assertions.assertEquals(1, SomeOtherEntity.deleteAll());

        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction4")
    public String transactionTest4() {
        // check that the third transaction was not committed
        Assertions.assertEquals(1, SomeEntity.count());
        // now delete our entity
        Assertions.assertEquals(1, SomeEntity.deleteAll());

        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction4-cs")
    public String transactionTest4CS() {
        // check that the third transaction was not committed
        Assertions.assertEquals(1, SomeOtherEntity.count());
        // now delete our entity
        Assertions.assertEquals(1, SomeOtherEntity.deleteAll());

        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction-new-sync")
    public Uni<String> newTransactionPropagationSynchronous() throws SystemException {
        Uni<String> ret = Uni.createFrom().item("OK");
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);

        txBean.doInTx();

        // We should see the transaction already committed even if we're async
        Assertions.assertEquals(1, Person.deleteAll());
        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction-new-uni")
    public Uni<String> newTransactionPropagationWithUni() throws SystemException {
        Person entity = new Person();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);
        // our entity
        Assertions.assertEquals(1, Person.count());

        return txBean.doInTxUni()
                // this makes sure we get executed in another scheduler
                .emitOn(executor)
                .map(text -> {
                    // make sure we don't see the other transaction's entity
                    Transaction t2;
                    try {
                        t2 = Panache.getTransactionManager().getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    Assertions.assertEquals(t1, t2);
                    try {
                        Assertions.assertEquals(Status.STATUS_ACTIVE, t2.getStatus());
                    } catch (SystemException e) {
                        throw new AssertionError(e);
                    }
                    return text;
                });
    }

    @Transactional
    @GET
    @Path("/transaction-uni-2")
    public Uni<String> transactionPropagationWithUni() {
        Uni<String> ret = Uni.createFrom().item("OK");
        // now delete both entities
        Assertions.assertEquals(2, Person.deleteAll());
        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction-multi")
    @Stream(value = MODE.RAW)
    public Multi<String> transactionPropagationWithMulti() throws SystemException {
        Person entity = new Person();
        entity.name = "Stef";
        entity.persist();
        Transaction t1 = Panache.getTransactionManager().getTransaction();
        Assertions.assertNotNull(t1);
        // our entity
        Assertions.assertEquals(1, Person.count());

        return txBean.doInTxMulti()
                // this makes sure we get executed in another scheduler
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(text -> {
                    // make sure we don't see the other transaction's entity
                    Transaction t2;
                    try {
                        t2 = Panache.getTransactionManager().getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    Assertions.assertEquals(t1, t2);
                    try {
                        Assertions.assertEquals(Status.STATUS_ACTIVE, t2.getStatus());
                    } catch (SystemException e) {
                        throw new AssertionError(e);
                    }
                    return text;
                });
    }

    @Transactional
    @GET
    @Path("/transaction-multi-2")
    public Publisher<String> transactionPropagationWithMulti2() {
        Multi<String> ret = Multi.createFrom().item("OK");
        // now delete both entities
        Assertions.assertEquals(2, Person.deleteAll());
        return ret;
    }
}
