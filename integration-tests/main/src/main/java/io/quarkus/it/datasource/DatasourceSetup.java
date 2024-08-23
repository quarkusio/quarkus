package io.quarkus.it.datasource;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatasourceSetup {

    @Inject
    DataSource dataSource;

    @PostConstruct
    public void setup() throws Exception {

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try {
                    statement.execute("drop table a");
                    statement.execute("drop table tx");
                } catch (Exception ignored) {

                }
                statement.execute("create table a (b int)");
                statement.execute("create table tx (b int)");
            }
        }
    }

    public void doInit() {

    }
}
