package io.quarkus.it.hibernate.reactive.postgresql;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/tests")
public class HibernateReactiveTestEndpoint {

    @Inject
    CompletionStage<Stage.Session> stageSession;

    @Inject
    Uni<Mutiny.Session> mutinySession;

    // Injecting a Vert.x Pool is not required, it us only used to
    // independently validate the contents of the database for the test
    @Inject
    PgPool pgPool;

    @GET
    @Path("/reactiveFind")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<GuineaPig> reactiveFind() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return stageSession
                .thenCompose(session -> {
                    return populateDB().convert().toCompletionStage()
                            .thenCompose(junk -> session.find(GuineaPig.class, expectedPig.getId()));
                });
    }

    @GET
    @Path("/reactiveFindMutiny")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GuineaPig> reactiveFindMutiny() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return mutinySession
                .flatMap(session -> {
                    return populateDB()
                            .onItem().produceUni(junk -> session.find(GuineaPig.class, expectedPig.getId()));
                });
    }

    @GET
    @Path("/reactivePersist")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactivePersist() {
        return mutinySession
                .flatMap(s -> s.persist(new GuineaPig(10, "Tulip")))
                .flatMap(s -> s.flush())
                .flatMap(junk -> selectNameFromId(10));
    }

    @GET
    @Path("/reactiveRemoveTransientEntity")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveRemoveTransientEntity() {
        return mutinySession
                .flatMap(mutinySession -> {
                    return populateDB()
                            .flatMap(junk -> selectNameFromId(5))
                            .map(name -> {
                                if (name == null)
                                    throw new AssertionError("Database was not populated properly");
                                return name;
                            })
                            .flatMap(junk -> mutinySession.merge(new GuineaPig(5, "Aloi")))
                            .flatMap(aloi -> mutinySession.remove(aloi))
                            .flatMap(junk -> mutinySession.flush())
                            .flatMap(junk -> selectNameFromId(5))
                            .map(result -> {
                                if (result == null)
                                    return "OK";
                                else
                                    return result;
                            });
                });
    }

    @GET
    @Path("/reactiveRemoveManagedEntity")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveRemoveManagedEntity() {
        return mutinySession
                .flatMap(mutinySession -> {
                    return populateDB()
                            .flatMap(junk -> mutinySession.find(GuineaPig.class, 5))
                            .flatMap(aloi -> mutinySession.remove(aloi))
                            .flatMap(junk -> mutinySession.flush())
                            .flatMap(junk -> selectNameFromId(5))
                            .map(result -> {
                                if (result == null)
                                    return "OK";
                                else
                                    return result;
                            });
                });
    }

    @GET
    @Path("/reactiveUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveUpdate() {
        final String NEW_NAME = "Tina";
        return mutinySession
                .flatMap(mutinySession -> {
                    return populateDB()
                            .flatMap(junk -> mutinySession.find(GuineaPig.class, 5))
                            .map(pig -> {
                                if (NEW_NAME.equals(pig.getName()))
                                    throw new AssertionError("Pig already had name " + NEW_NAME);
                                pig.setName(NEW_NAME);
                                return pig;
                            })
                            .flatMap(junk -> mutinySession.flush())
                            .flatMap(junk -> selectNameFromId(5));
                });
    }

    private Uni<RowSet<Row>> populateDB() {
        return pgPool.getConnection()
                .flatMap(c -> c.preparedQuery("DELETE FROM Pig").execute().map(junk -> c))
                .flatMap(c -> c.preparedQuery("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')").execute());
    }

    private Uni<String> selectNameFromId(Integer id) {
        return pgPool.preparedQuery("SELECT name FROM Pig WHERE id = $1").execute(Tuple.of(id)).map(rowSet -> {
            if (rowSet.size() == 1) {
                return rowSet.iterator().next().getString(0);
            } else if (rowSet.size() > 1) {
                throw new AssertionError("More than one result returned: " + rowSet.size());
            } else {
                return null; // Size 0
            }
        });
    }

}
