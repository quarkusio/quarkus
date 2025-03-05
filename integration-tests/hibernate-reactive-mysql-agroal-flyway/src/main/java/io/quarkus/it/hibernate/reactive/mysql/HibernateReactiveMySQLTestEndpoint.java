package io.quarkus.it.hibernate.reactive.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.reactive.mutiny.Mutiny;

import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

@Path("/tests")
public class HibernateReactiveMySQLTestEndpoint {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    // Injecting a Vert.x Pool is not required, it us only used to
    // independently validate the contents of the database for the test
    @Inject
    Pool mysqlPool;

    @Inject
    AgroalDataSource jdbcDataSource;

    @GET
    @Path("/jdbcFind")
    public GuineaPig jdbcFind() throws SQLException {
        final GuineaPig expectedPig = new GuineaPig(6, "Iola");
        populateDBJdbc();
        return selectJdbc(6);
    }

    @GET
    @Path("/reactiveFindMutiny")
    public Uni<GuineaPig> reactiveFindMutiny() {
        final GuineaPig expectedPig = new GuineaPig(5, "Aloi");
        return populateDB()
                .chain(() -> sessionFactory.withSession(s -> s.find(GuineaPig.class, expectedPig.getId())));
    }

    @GET
    @Path("/reactivePersist")
    public Uni<String> reactivePersist() {
        return sessionFactory.withTransaction(s -> s.persist(new GuineaPig(10, "Tulip")))
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
                .chain(() -> sessionFactory
                        .withTransaction(s -> s.merge(new GuineaPig(5, "Aloi")).chain(s::remove)))
                .chain(() -> selectNameFromId(5))
                .onItem().ifNotNull().transform(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveRemoveManagedEntity")
    public Uni<String> reactiveRemoveManagedEntity() {
        return populateDB()
                .chain(() -> sessionFactory.withTransaction(s -> s.find(GuineaPig.class, 5).chain(s::remove)))
                .chain(() -> selectNameFromId(5))
                .onItem().ifNotNull().transform(result -> result)
                .onItem().ifNull().continueWith("OK");
    }

    @GET
    @Path("/reactiveUpdate")
    public Uni<String> reactiveUpdate() {
        final String NEW_NAME = "Tina";
        return populateDB()
                .chain(() -> sessionFactory.withTransaction(s -> s.find(GuineaPig.class, 5)
                        .invoke(pig -> {
                            if (NEW_NAME.equals(pig.getName())) {
                                throw new AssertionError("Pig already had name " + NEW_NAME);
                            }
                            pig.setName(NEW_NAME);
                        })))
                .chain(() -> selectNameFromId(5));
    }

    private Uni<RowSet<Row>> populateDB() {
        return mysqlPool.query("DELETE FROM Pig").execute()
                .flatMap(junk -> mysqlPool.preparedQuery("INSERT INTO Pig (id, name) VALUES (5, 'Aloi')").execute());
    }

    private void populateDBJdbc() throws SQLException {
        Connection connection = jdbcDataSource.getConnection();
        connection.prepareStatement("DELETE FROM Pig").execute();
        connection.prepareStatement("INSERT INTO Pig (id, name) VALUES (6, 'Iola')").execute();
        connection.close();
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

    private GuineaPig selectJdbc(Integer id) throws SQLException {
        Connection connection = jdbcDataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM Pig WHERE id = ?");
        statement.setInt(1, id);
        ResultSet rowSet = statement.executeQuery();
        rowSet.next();
        return new GuineaPig(rowSet.getInt("id"), rowSet.getString("name"));
    }

}
