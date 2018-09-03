package org.jboss.shamrock.example.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/datasource")
public class DatasourceResource {
//
//    @Inject
//    private DataSource dataSource;
//
//    @GET
//    public String simpleTest() throws Exception {
//        try (Connection con = dataSource.getConnection()) {
//            try (Statement statement = con.createStatement()) {
//                statement.execute("create table a (b int)");
//            }
//            try (Statement statement = con.createStatement()) {
//                statement.execute("insert into a values (10)");
//            }
//            try (Statement statement = con.createStatement()) {
//                try (ResultSet rs = statement.executeQuery("select b from a")) {
//                    if(rs.next()) {
//                        return rs.getString(1);
//                    }
//                    return "FAILED";
//                }
//            }
//        }
//    }

}
