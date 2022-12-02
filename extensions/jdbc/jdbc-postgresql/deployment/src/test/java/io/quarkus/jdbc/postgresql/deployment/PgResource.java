package io.quarkus.jdbc.postgresql.deployment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.agroal.api.AgroalDataSource;
import io.smallrye.common.annotation.Blocking;

@Path("/pg")
@Blocking
public class PgResource {

    @Inject
    AgroalDataSource ds;

    @PostConstruct
    @Transactional
    public void setup() throws Exception {
        try (var con = ds.getConnection()) {
            try (var smt = con.createStatement()) {
                smt.executeUpdate("create table foo (name varchar(100) primary key not null, value varchar(100));");
            }
        }
    }

    @GET
    @Path("save")
    public void save(@QueryParam("name") String name, @QueryParam("value") String value) throws Exception {
        try (var con = ds.getConnection()) {
            try (var smt = con.prepareStatement("insert into foo (name, value) values (?,?)")) {
                smt.setString(1, name);
                smt.setString(2, value);
                smt.execute();
            }
        }
    }

    @GET
    @Path("get")
    public String get(@QueryParam("name") String name) throws Exception {
        try (var con = ds.getConnection()) {
            try (var smt = con.prepareStatement("select (value) from foo where name = ?")) {
                smt.setString(1, name);
                try (var rs = smt.executeQuery()) {
                    if (!rs.next()) {
                        throw new NotFoundException();
                    }
                    return rs.getString(1);
                }
            }
        }
    }
}
