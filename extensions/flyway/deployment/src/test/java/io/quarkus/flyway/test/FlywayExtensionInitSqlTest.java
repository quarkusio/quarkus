package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionInitSqlTest {
    // Quarkus built object
    @Inject
    DataSource datasource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("init-sql-config.properties", "application.properties"));

    @Test
    @DisplayName("Check if initSql is invoked")
    public void testFlywayInitSql() throws SQLException {
        int var = 0;
        try (Connection con = datasource.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT ONE_HUNDRED");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                var = rs.getInt(1);
            }
        }
        assertEquals(100, var, "Init SQL was not executed");
    }
}
