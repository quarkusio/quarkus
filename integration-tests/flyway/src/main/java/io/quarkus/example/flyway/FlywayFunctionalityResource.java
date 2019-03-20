/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.example.flyway;

import static java.sql.DriverManager.getConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

@Path("/flyway")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FlywayFunctionalityResource {

    @Inject
    @ConfigProperty(name = "quarkus.datasource.url")
    String dbURL;
    @Inject
    @ConfigProperty(name = "quarkus.datasource.username")
    String dbUser;
    @Inject
    @ConfigProperty(name = "quarkus.datasource.password")
    String dbPassword;

    @GET
    @Path("/migrate")
    public String doMigrate() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(dbURL, dbUser, dbPassword)
                .load();
        flyway.migrate();
        int cols = countTableRows();
        return "OK " + cols;
    }

    private int countTableRows() throws SQLException {
        try (Connection connection = getConnection(dbURL, dbUser, dbPassword)) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet execute = statement.executeQuery("SELECT COUNT(*) FROM quarkus; ")) {
                    if (execute.next()) {
                        return execute.getInt(1);
                    } else {
                        return -1; //ERROR
                    }
                }
            }
        }
    }
}
