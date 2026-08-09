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

import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesPostgresqlDatasourceWithMountTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            // The official postgres image will execute all the scripts in the folder "docker-entrypoint-initdb.d"
            .overrideConfigKey("quarkus.datasource.devservices.mounts[0].type", "classpath")
            .overrideConfigKey("quarkus.datasource.devservices.mounts[0].source", "./init-db.sql")
            .overrideConfigKey("quarkus.datasource.devservices.mounts[0].target", "/docker-entrypoint-initdb.d/init-db.sql")
            .overrideConfigKey("quarkus.datasource.devservices.mounts[0].read-only", "true");

    @Inject
    DataSource ds;

    @Test
    @DisplayName("Test if the structured mount is applied successfully")
    public void testDatasource() throws Exception {
        int result = 0;
        try (Connection con = ds.getConnection();
                CallableStatement cs = con.prepareCall("SELECT my_func()");
                ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                result = rs.getInt(1);
            }
        }
        assertEquals(100, result, "The init script should have been executed");
    }
}
