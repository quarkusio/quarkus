package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigUrlMissingDefaultDatasourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        assertThatThrownBy(() -> myBean.useDatasource())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("quarkus.datasource.jdbc.url has not been defined");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        DataSource ds;

        public void useDatasource() throws SQLException {
            ds.getConnection();
        }
    }
}
