package io.quarkus.it.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/datasource")
public class DatasourceResource {

    @Inject
    DataSource dataSource;

    @Inject
    UserTransaction userTransaction;

    @Inject
    DatasourceSetup datasourceSetup;

    @PostConstruct
    void postConstruct() throws Exception {
        datasourceSetup.doInit();
    }

    @GET
    public String simpleTest() throws Exception {
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.execute("insert into a values (10)");
            }
            try (Statement statement = con.createStatement()) {
                try (ResultSet rs = statement.executeQuery("select b from a")) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    return "FAILED";
                }
            }
        }
    }

    @GET
    @Path("/txn")
    public String transactionTest() throws Exception {
        userTransaction.begin();
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.execute("insert into tx values (10)");
            }
        }
        userTransaction.rollback();
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet rs = statement.executeQuery("select b from a")) {
                    if (rs.next()) {
                        return "FAILED";
                    }
                    return "PASSED";
                }
            }
        }
    }

    @GET
    @Path("/txninterceptor0")
    @Transactional(value = Transactional.TxType.REQUIRED)
    public String transactionInterceptorTest0() throws Exception {
        if (userTransaction.getStatus() != Status.STATUS_ACTIVE) {
            return "FAILED";
        }
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.execute("insert into tx values (1234)");
            }
        }
        return "PASSED";
    }

    @GET
    @Path("/txninterceptor1")
    @Transactional(value = Transactional.TxType.REQUIRED)
    public String transactionInterceptorTest1() throws Exception {
        if (userTransaction.getStatus() != Status.STATUS_ACTIVE) {
            return "FAILED";
        }
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.execute("insert into tx values (787)");
            }
        }
        throw new RuntimeException("ROLLBACK");
    }

    @GET
    @Path("/txninterceptor2")
    @Transactional(value = Transactional.TxType.REQUIRED)
    public String transactionInterceptorTest2(@QueryParam("val") long val) throws Exception {
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet rs = statement.executeQuery("select b from tx where b=787")) {
                    if (rs.next()) {
                        return "FAILED: 787 was present even though TX rolled back";
                    }
                }
            }
            try (Statement statement = con.createStatement()) {
                try (ResultSet rs = statement.executeQuery("select b from tx where b=1234")) {
                    if (!rs.next()) {
                        return "FAILED: 1234 was not present";
                    }
                }
            }
            return "PASSED";
        }
    }
}
