package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionDifferentUsernameTest {

    @Inject
    DataSource datasource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar

                    .addAsResource("config-for-different-username.properties", "application.properties"));

    @Test
    @DisplayName("Check if connected with new username")
    public void testFlywayInitSql() throws SQLException {

        int var = 0;
        try (Connection con = datasource.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT 100");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                var = rs.getInt(1);
            }
        }
        assertEquals(100, var, "Different username and password was not executed");

    }

}
