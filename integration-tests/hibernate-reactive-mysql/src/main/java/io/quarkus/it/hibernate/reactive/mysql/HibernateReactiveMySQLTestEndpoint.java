package io.quarkus.it.hibernate.reactive.mysql;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/tests")
public class HibernateReactiveMySQLTestEndpoint {

    @Inject
    Stage.Session stageSession;

    @Inject
    Mutiny.Session mutinySession;

    // Injecting a Vert.x Pool is not required, it us only used to
    // independently validate the contents of the database for the test
    @Inject
    MySQLPool mysqlPool;

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
                .chain(() -> mutinySession.find(GuineaPig.class, expectedPig.getId()));
    }

    @GET
    @Path("/reactivePersist")
    public Uni<String> reactivePersist() {
        return mutinySession.persist(new GuineaPig(10, "Tulip"))
                .chain(() -> mutinySession.flush())
                .chain(() -> selectNameFromId(10));
    }

    @GET
    @Path("/reactiveRemoveTransientEntity")
    public Uni<String> reactiveRemoveTransientEntity() {
        return populateDB()
                .chain(() -> selectNameFromId(5))
                .map(name -> {
                    if (name == null) {
                        throw new AssertionError("Database was not populated properly");
                    }
                    return name;
                })
                .chain(() -> mutinySession.merge(new GuineaPig(5, "Aloi")))
                .chain(aloi -> mutinySession.remove(aloi))
                .chain(() -> mutinySession.flush())
                .chain(() -> selectNameFromId(5))
                .onItem().ifNotNull().transform(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveRemoveManagedEntity")
    public Uni<String> reactiveRemoveManagedEntity() {
        return populateDB()
                .chain(() -> mutinySession.find(GuineaPig.class, 5))
                .chain(aloi -> mutinySession.remove(aloi))
                .chain(() -> mutinySession.flush())
                .chain(() -> selectNameFromId(5))
                .onItem().ifNotNull().transform(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveUpdate")
    public Uni<String> reactiveUpdate() {
        final String NEW_NAME = "Tina";
        return populateDB()
                .chain(() -> mutinySession.find(GuineaPig.class, 5))
                .map(pig -> {
                    if (NEW_NAME.equals(pig.getName())) {
                        throw new AssertionError("Pig already had name " + NEW_NAME);
                    }
                    pig.setName(NEW_NAME);
                    return pig;
                })
                .chain(() -> mutinySession.flush())
                .chain(() -> selectNameFromId(5));
    }

    private Uni<RowSet<Row>> populateDB() {
        return mysqlPool.query("DELETE FROM Pig").execute()
                .flatMap(junk -> mysqlPool.preparedQuery("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')").execute());
    }

    private Uni<String> selectNameFromId(Integer id) {
        return mysqlPool.preparedQuery("SELECT name FROM Pig WHERE id = ?").execute(Tuple.of(id)).map(rowSet -> {
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
