package io.quarkus.it.neo4j;

import static javax.ws.rs.core.MediaType.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.StatementResultCursor;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxStatementResult;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

@Path("/neo4j")
public class Neo4jResource {

    @Inject
    Driver driver;

    @GET
    @Path("/blocking")
    public String doStuffWithNeo4j() {
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
    @Path("/asynchronous")
    @Produces(APPLICATION_JSON)
    public CompletionStage<List<Integer>> doStuffWithNeo4jAsynchronous() {
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
    public Publisher<Integer> doStuffWithNeo4jReactive() {

        return Flux.using(driver::rxSession, session -> session.readTransaction(tx -> {
            RxStatementResult result = tx.run("UNWIND range(1, 3) AS x RETURN x", Collections.emptyMap());
            return Flux.from(result.records()).map(record -> record.get("x").asInt());
        }), RxSession::close);
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
            StatementResult result = transaction
                    .run("MATCH (f:Framework {name: $name}) - [:CAN_USE] -> (n) RETURN f, n",
                            Values.parameters("name", "Quarkus"));
            result.forEachRemaining(
                    record -> System.out.println(String.format("%s works with %s", record.get("n").get("name").asString(),
                            record.get("f").get("name").asString())));
            transaction.commit();
        }
    }

    private static void readNodesAsync(Driver driver) {
        AsyncSession session = driver.asyncSession();
        session
                .runAsync("UNWIND range(1, 3) AS x RETURN x")
                .thenCompose(StatementResultCursor::listAsync)
                .whenComplete((records, error) -> {
                    if (records != null) {
                        System.out.println(records);
                    } else {
                        error.printStackTrace();
                    }
                })
                .thenCompose(records -> {
                    System.out.println("clsoing!!!");
                    return session.closeAsync()
                            .thenApply(ignore -> records);
                });
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
