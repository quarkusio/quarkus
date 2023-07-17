package io.quarkus.config.yaml.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public class InterfaceConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, SqlConfiguration.class, SqlConfiguration.Type.class)
                    .addAsResource("configprops.yaml", "application.yaml"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        SqlConfiguration sqlConfiguration = dummyBean.sqlConfiguration;
        assertEquals("defaultName", sqlConfiguration.name());
        assertEquals("defaultUser", sqlConfiguration.user());
        assertEquals("defaultPassword", sqlConfiguration.password());
        assertEquals(100, sqlConfiguration.maxPoolSize());
        assertEquals(200, sqlConfiguration.maxIdleTimeSeconds());
        assertEquals(SqlConfiguration.Type.DEFAULT_TYPE, sqlConfiguration.type());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        SqlConfiguration sqlConfiguration;
    }

    @ConfigMapping(prefix = "sql")
    public interface SqlConfiguration {

        @WithName("max_pool_size")
        @WithDefault("50")
        int maxPoolSize();

        @WithName("max_idle_time_seconds")
        @WithDefault("100")
        int maxIdleTimeSeconds();

        String name();

        String user();

        String password();

        Type type();

        enum Type {
            MSSQL,
            POSTGRES,
            MYSQL,
            DEFAULT_TYPE
        }
    }

}
