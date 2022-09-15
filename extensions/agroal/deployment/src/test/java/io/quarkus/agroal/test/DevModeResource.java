package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/dev")
public class DevModeResource {

    @Inject
    DataSource dataSource;

    @GET
    @Path("/user")
    public String getUser() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery("SELECT USER()")) {
                    rs.next();
                    return rs.getString(1);
                }
            }
        }
    }
}
