package io.quarkus.it.opentracing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/tracingpropertyjdbc")
public class TracingProperyJdbcResource {
    @Inject
    @io.quarkus.agroal.DataSource("postgres")
    DataSource postgresDataSource;

    @Inject
    @io.quarkus.agroal.DataSource("postgres2")
    DataSource postgresDataSource2;

    @Inject
    @io.quarkus.agroal.DataSource("postgres3")
    DataSource postgresDataSource3;

    @Inject
    @io.quarkus.agroal.DataSource("postgres4")
    DataSource postgresDataSource4;

    @GET()
    @Path("tracingenabled")
    public TraceData jdbcTracingEnabled() throws SQLException {
        Connection con = postgresDataSource.getConnection();
        try (Statement stmt = con.createStatement()) {
            ResultSet resultSet = stmt.executeQuery("select 1");
            resultSet.next();
            String result = resultSet.getString(1);
            TraceData data = new TraceData();
            data.message = result;
            return data;
        }
    }

    @GET()
    @Path("tracingdisabled")
    public TraceData jdbcTracingDisabled() throws SQLException {
        Connection con = postgresDataSource2.getConnection();
        try (Statement stmt = con.createStatement()) {
            ResultSet resultSet = stmt.executeQuery("select 1");
            resultSet.next();
            String result = resultSet.getString(1);
            TraceData data = new TraceData();
            data.message = result;
            return data;
        }
    }

    @GET()
    @Path("traceactivespanonly")
    public TraceData traceActiveSpanOnly() throws SQLException {
        Connection con = postgresDataSource3.getConnection();
        try (Statement stmt = con.createStatement()) {
            ResultSet resultSet = stmt.executeQuery("select 1");
            resultSet.next();
            String result = resultSet.getString(1);
            TraceData data = new TraceData();
            data.message = result;
            return data;
        }
    }

    @GET()
    @Path("traceignoresql")
    public TraceData traceIgnoreSql() throws SQLException {
        Connection con = postgresDataSource4.getConnection();
        try (Statement stmt = con.createStatement()) {
            ResultSet resultSet = stmt.executeQuery("select 1");
            resultSet.next();
            String result = resultSet.getString(1);
            TraceData data = new TraceData();
            data.message = result;
            return data;
        }
    }
}
