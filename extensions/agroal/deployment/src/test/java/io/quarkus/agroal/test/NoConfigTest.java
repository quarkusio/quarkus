package io.quarkus.agroal.test;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Test
    public void testNoConfig() throws SQLException {
        // we should be able to start the application, even with no configuration at all
    }
}
