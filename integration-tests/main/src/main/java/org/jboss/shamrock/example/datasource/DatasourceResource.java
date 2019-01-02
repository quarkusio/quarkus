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

package org.jboss.shamrock.example.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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
