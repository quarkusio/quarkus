package org.jboss.shamrock.example.datasource;

import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

@ApplicationScoped
public class DatasourceSetup {

    @Inject
    private DataSource dataSource;

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
