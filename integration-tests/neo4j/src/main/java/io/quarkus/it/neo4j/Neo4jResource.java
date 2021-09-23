package io.quarkus.it.neo4j;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.SERVER_SENT_EVENTS;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

@Path("/neo4j")
public class Neo4jResource {

    @Inject
    Driver driver;

    @Inject
    UserTransaction ut;

    @GET
    @Path("/blocking")
    @Produces(TEXT_PLAIN)
    public String blocking() {
        try {
            createNodes(driver);

            readNodes(driver);
        } catch (Exception e) {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            reportException("An error occurred while performing Neo4j operations", e, writer);
            writer.flush();
            writer.close();
            return out.toString();
        }
        return "OK";
    }

    @GET
    @Path("/blockingWithJTATransactional")
    @Produces(TEXT_PLAIN)
    @Transactional
    public String blockingWithJTATransactional(@QueryParam("externalId") String externalId,
            @QueryParam("causeRollback") @DefaultValue("false") boolean causeRollback) {
        try (Session session = driver.session()) {
            session.run(
                    "CREATE (f:Framework {name: $name, id: $id}) - [:CAN_USE {transactional: 'of course'}] -> (n:Database {name: 'Neo4j'})",
                    Values.parameters("name", "Quarkus", "id", externalId));
        }
        if (causeRollback) {
            throw new SomeException("On purpose.");
        }
        return "OK";
    }

    @GET
    @Path("/explicitTransactions")
    @Produces(TEXT_PLAIN)
    public Response explicitTransactions(
            @QueryParam("externalId") String externalId,
            @QueryParam("causeRollback") @DefaultValue("false") boolean causeRollback) {
        try {
            ut.begin();

            try (Session session = driver.session()) {
                session.run(
                        "CREATE (f:Framework {name: $name, id: $id}) - [:CAN_USE {transactional: 'of course'}] -> (n:Database {name: 'Neo4j'})",
                        Values.parameters("name", "Quarkus", "id", externalId));
            }

            if (causeRollback) {
                ut.rollback();
            } else {
                ut.commit();
            }

        } catch (NotSupportedException | SystemException | HeuristicRollbackException | HeuristicMixedException
                | RollbackException e) {
            throw new RuntimeException(e);
        }

        if (causeRollback) {
            return Response.serverError().entity("On purpose.").build();
        }

        return Response.ok("OK").build();
    }

    @GET
    @Path("/mixingUpTxConcepts")
    @Produces(TEXT_PLAIN)
    @Transactional
    public String mixingUpTxConcepts() {
        try (Session session = driver.session();
                Transaction transaction = session.beginTransaction()) {
            transaction.run("CREATE (f:Framework {name: $name}) - [:CAN_USE] -> (n:Database {name: 'Neo4j'})",
                    Values.parameters("name", "Quarkus"));
            transaction.commit();
        } catch (Exception e) {
            throw new SomeException(e.getMessage());
        }
        return "OK";
    }

    public static class SomeException extends RuntimeException {
        public SomeException(String message) {
            super(message);
        }
    }

    @Provider
    public static class IllegalArgumentExceptionMapper implements ExceptionMapper<SomeException> {

        @Override
        public Response toResponse(SomeException exception) {

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exception.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/asynchronous")
    @Produces(APPLICATION_JSON)
    public CompletionStage<List<Integer>> asynchronous() {
        AsyncSession session = driver.asyncSession();
        return session
                .runAsync("UNWIND range(1, 3) AS x RETURN x")
                .thenCompose(cursor -> cursor.listAsync(record -> record.get("x").asInt()))
                .whenComplete((records, error) -> {
                    if (records != null) {
                        System.out.println(records);
                    } else {
                        error.printStackTrace();
                    }
                })
                .thenCompose(records -> session.closeAsync()
                        .thenApply(ignore -> records));
    }

    @GET
    @Path("/reactive")
    @Produces(SERVER_SENT_EVENTS)
    public Publisher<Integer> reactive() {

        return Flux.using(driver::rxSession, session -> session.readTransaction(tx -> {
            RxResult result = tx.run("UNWIND range(1, 3) AS x RETURN x", Collections.emptyMap());
            return Flux.from(result.records()).map(record -> record.get("x").asInt());
        }), RxSession::close).doOnNext(System.out::println);
    }

    @GET
    @Path("/countNodesWith")
    @Produces(TEXT_PLAIN)
    public long countNodesWith(@QueryParam("externalId") String externalId) {
        try (var session = driver.session()) {
            return session.run("MATCH (n:Framework {id: $id}) RETURN count(n)", Values.parameters("id", externalId))
                    .single().get(0).asLong();
        }
    }

    private static void createNodes(Driver driver) {
        try (Session session = driver.session();
                Transaction transaction = session.beginTransaction()) {
            transaction.run("CREATE (f:Framework {name: $name}) - [:CAN_USE] -> (n:Database {name: 'Neo4j'})",
                    Values.parameters("name", "Quarkus"));
            transaction.commit();
        }
    }

    private static void readNodes(Driver driver) {
        try (Session session = driver.session();
                Transaction transaction = session.beginTransaction()) {
            Result result = transaction
                    .run("MATCH (f:Framework {name: $name}) - [:CAN_USE] -> (n) RETURN f, n",
                            Values.parameters("name", "Quarkus"));
            result.forEachRemaining(
                    record -> System.out.println(String.format("%s works with %s", record.get("n").get("name").asString(),
                            record.get("f").get("name").asString())));
            transaction.commit();
        }
    }

    private void reportException(String errorMessage, final Exception e, final PrintWriter writer) {
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }
}
