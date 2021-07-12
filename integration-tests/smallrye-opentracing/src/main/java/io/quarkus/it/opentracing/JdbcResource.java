package io.quarkus.it.opentracing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
