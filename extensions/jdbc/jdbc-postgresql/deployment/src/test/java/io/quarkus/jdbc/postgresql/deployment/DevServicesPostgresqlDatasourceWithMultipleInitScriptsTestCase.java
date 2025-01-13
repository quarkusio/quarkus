package io.quarkus.jdbc.postgresql.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DevServicesPostgresqlDatasourceWithMultipleInitScriptsTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addAsResource("init-db.sql"))
            .withApplicationRoot(javaArchive -> javaArchive.addAsResource("init-db-2.sql"))
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.devservices.init-script-path", "init-db.sql, init-db-2.sql");

    @Inject
    DataSource ds;

    @Test
    @DisplayName("Test if init-script-path executed successfully")
    public void testDatasource() throws Exception {
        int result = 0;
        try (Connection con = ds.getConnection();
                CallableStatement cs = con.prepareCall("SELECT my_func()");
                ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                result = rs.getInt(1);
            }
        }
        assertEquals(100, result, "The first init script should have been executed");
        try (Connection con = ds.getConnection();
                CallableStatement cs = con.prepareCall("SELECT my_func_2()");
                ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                result = rs.getInt(1);
            }
        }
        assertEquals(200, result, "The second init script should have been executed");
    }
}
