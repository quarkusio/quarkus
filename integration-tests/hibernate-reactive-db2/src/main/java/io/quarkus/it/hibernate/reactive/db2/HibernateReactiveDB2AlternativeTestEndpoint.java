package io.quarkus.it.hibernate.reactive.db2;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.db2client.DB2Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/alternative-tests")
@Produces(MediaType.APPLICATION_JSON)
public class HibernateReactiveDB2AlternativeTestEndpoint {

    @Inject
    Stage.Session stageSession;

    @Inject
    Mutiny.Session mutinySession;

    // Injecting a Vert.x Pool is not required, it us only used to
    // independently validate the contents of the database for the test
    @Inject
    DB2Pool db2Pool;

    @GET
    @Path("/reactiveFind")
    public CompletionStage<GuineaPig> reactiveFind() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB().convert().toCompletionStage()
                .thenCompose(junk -> stageSession.find(GuineaPig.class, expectedPig.getId()));
    }

    @GET
    @Path("/reactiveFindMutiny")
    public Uni<GuineaPig> reactiveFindMutiny() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB()
                .onItem().produceUni(junk -> mutinySession.find(GuineaPig.class, expectedPig.getId()));
    }

    @GET
    @Path("/reactivePersist")
    public Uni<String> reactivePersist() {
        return mutinySession.persist(new GuineaPig(10, "Tulip"))
                .onItem().produceUni(s -> s.flush())
                .onItem().produceUni(junk -> selectNameFromId(10));
    }

    @GET
    @Path("/reactiveRemoveTransientEntity")
    public Uni<String> reactiveRemoveTransientEntity() {
        return populateDB()
                .onItem().produceUni(junk -> selectNameFromId(5))
                .onItem().apply(name -> {
                    if (name == null) {
                        throw new AssertionError("Database was not populated properly");
                    }
                    return name;
                })
                .onItem().produceUni(junk -> mutinySession.merge(new GuineaPig(5, "Aloi")))
                .onItem().produceUni(aloi -> mutinySession.remove(aloi))
                .onItem().produceUni(junk -> mutinySession.flush())
                .onItem().produceUni(junk -> selectNameFromId(5))
                .onItem().ifNotNull().apply(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveRemoveManagedEntity")
    public Uni<String> reactiveRemoveManagedEntity() {
        return populateDB()
                .onItem().produceUni(junk -> mutinySession.find(GuineaPig.class, 5))
                .onItem().produceUni(aloi -> mutinySession.remove(aloi))
                .onItem().produceUni(junk -> mutinySession.flush())
                .onItem().produceUni(junk -> selectNameFromId(5))
                .onItem().ifNotNull().apply(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveUpdate")
    public Uni<String> reactiveUpdate() {
        final String NEW_NAME = "Tina";
        return populateDB()
                .onItem().produceUni(junk -> mutinySession.find(GuineaPig.class, 5))
                .onItem().apply(pig -> {
                    if (NEW_NAME.equals(pig.getName())) {
                        throw new AssertionError("Pig already had name " + NEW_NAME);
                    }
                    pig.setName(NEW_NAME);
                    return pig;
                })
                .onItem().produceUni(junk -> mutinySession.flush())
                .onItem().produceUni(junk -> selectNameFromId(5));
    }

    private Uni<RowSet<Row>> populateDB() {
        return db2Pool.query("DELETE FROM Pig").execute()
                .flatMap(junk -> db2Pool.preparedQuery("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')").execute());
    }

    private Uni<String> selectNameFromId(Integer id) {
        return db2Pool.preparedQuery("SELECT name FROM Pig WHERE id = ?").execute(Tuple.of(id)).map(rowSet -> {
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
