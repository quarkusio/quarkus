package io.quarkus.it.opentracing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/jdbc")
public class JdbcResource {
    @Inject
    DataSource defaultDataSource;

    @GET
    public TraceData jdbc() throws SQLException {
        Connection con = defaultDataSource.getConnection();
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
