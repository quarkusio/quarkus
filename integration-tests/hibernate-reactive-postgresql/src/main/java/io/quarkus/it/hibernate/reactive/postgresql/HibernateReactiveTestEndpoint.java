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
    Stage.Session stageSession;

    @Inject
    Mutiny.Session mutinySession;

    // Injecting a Vert.x Pool is not required, it us only used to
    // independently validate the contents of the database for the test
    @Inject
    PgPool pgPool;

    @GET
    @Path("/reactiveFind")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<GuineaPig> reactiveFind() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB().convert().toCompletionStage()
                .thenCompose(junk -> stageSession.find(GuineaPig.class, expectedPig.getId()));
    }

    @GET
    @Path("/reactiveFindMutiny")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<GuineaPig> reactiveFindMutiny() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB().then(() -> mutinySession.find(GuineaPig.class, expectedPig.getId()));
    }

    @GET
    @Path("/reactivePersist")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactivePersist() {
        final GuineaPig pig = new GuineaPig(10, "Tulip");
        return mutinySession
                .persist(pig)
                .then(() -> mutinySession.flush())
                .then(() -> selectNameFromId(10));
    }

    @GET
    @Path("/reactiveCowPersist")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<FriesianCow> reactiveCowPersist() {
        final FriesianCow cow = new FriesianCow();
        cow.name = "Carolina";
        return mutinySession
                .persist(cow)
                .then(() -> mutinySession.flush())
                .flatMap(s -> s.createQuery("from FriesianCow f where f.name = :name", FriesianCow.class)
                        .setParameter("name", cow.name).getSingleResult());
    }

    @GET
    @Path("/reactiveRemoveTransientEntity")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveRemoveTransientEntity() {
        return populateDB()
                .then(() -> selectNameFromId(5))
                .invoke(name -> {
                    if (name == null)
                        throw new AssertionError("Database was not populated properly");
                })
                .then(() -> mutinySession.merge(new GuineaPig(5, "Aloi")))
                .invoke(aloi -> mutinySession.remove(aloi))
                .then(() -> mutinySession.flush())
                .then(() -> selectNameFromId(5))
                .map(result -> {
                    if (result == null)
                        return "OK";
                    else
                        return result;
                });
    }

    @GET
    @Path("/reactiveRemoveManagedEntity")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveRemoveManagedEntity() {
        return populateDB()
                .then(() -> mutinySession.find(GuineaPig.class, 5))
                .chain(aloi -> mutinySession.remove(aloi))
                .then(() -> mutinySession.flush())
                .then(() -> selectNameFromId(5))
                .map(result -> {
                    if (result == null)
                        return "OK";
                    else
                        return result;
                });
    }

    @GET
    @Path("/reactiveUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> reactiveUpdate() {
        final String NEW_NAME = "Tina";
        return populateDB()
                .then(() -> mutinySession.find(GuineaPig.class, 5))
                .invoke(pig -> {
                    if (NEW_NAME.equals(pig.getName()))
                        throw new AssertionError("Pig already had name " + NEW_NAME);
                    pig.setName(NEW_NAME);
                })
                .then(() -> mutinySession.flush())
                .then(() -> selectNameFromId(5));
    }

    private Uni<RowSet<Row>> populateDB() {
        return Uni.combine().all().unis(
                pgPool.query("DELETE FROM Pig").execute(),
                pgPool.query("DELETE FROM Cow").execute())
                .asTuple()
                .then(() -> pgPool.preparedQuery("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')").execute());
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
