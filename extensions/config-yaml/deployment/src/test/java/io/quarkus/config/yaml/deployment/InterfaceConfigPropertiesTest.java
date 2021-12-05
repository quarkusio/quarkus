package io.quarkus.config.yaml.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, SqlConfiguration.class, SqlConfiguration.Nested.class,
                            SqlConfiguration.Type.class)
                    .addAsResource("configprops.yaml", "application.yaml"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        SqlConfiguration sqlConfiguration = dummyBean.sqlConfiguration;
        assertEquals("defaultName", sqlConfiguration.getName());
        assertEquals("defaultUser", sqlConfiguration.getUser());
        assertEquals("defaultPassword", sqlConfiguration.getPassword());
        assertEquals(100, sqlConfiguration.maxPoolSize());
        assertEquals(200, sqlConfiguration.maxIdleTimeSeconds());
        assertEquals(SqlConfiguration.Type.DEFAULT_TYPE, sqlConfiguration.getType());
    }

    @Singleton
    public static class DummyBean {
        @Inject
        SqlConfiguration sqlConfiguration;
    }

    @ConfigProperties(prefix = "sql")
    public interface SqlConfiguration {
        @ConfigProperty(name = "max_pool_size", defaultValue = "50")
        int maxPoolSize();

        @ConfigProperty(name = "max_idle_time_seconds", defaultValue = "100")
        int maxIdleTimeSeconds();

        String getName();

        String getUser();

        String getPassword();

        Type getType();

        class Nested {
            @ConfigProperty(name = "max_pool_size")
            private int maxPoolSize = 50;
            @ConfigProperty(name = "max_idle_time_seconds")
            public int maxIdleTimeSeconds = 150;
            public String name;
            public String user;
            public String password;
            private Type type;

            public int getMaxPoolSize() {
                return maxPoolSize;
            }

            public void setMaxPoolSize(int maxPoolSize) {
                this.maxPoolSize = maxPoolSize;
            }

            public Type getType() {
                return type;
            }

            public void setType(Type type) {
                this.type = type;
            }
        }

        enum Type {
            MSSQL,
            POSTGRES,
            MYSQL,
            DEFAULT_TYPE
        }
    }

}
