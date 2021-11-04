package io.quarkus.config.yaml.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyBean.class, SqlConfiguration.class, Nested.class)
                    .addAsResource("configprops.yaml", "application.yaml"));

    @Inject
    DummyBean dummyBean;

    @Test
    public void testConfiguredValues() {
        SqlConfiguration sqlConfiguration = dummyBean.sqlConfiguration;
        assertEquals("defaultName", sqlConfiguration.name);
        assertEquals("defaultUser", sqlConfiguration.user);
        assertEquals("defaultPassword", sqlConfiguration.getPassword());
        assertEquals(100, sqlConfiguration.maxPoolSize);
        assertEquals(200, sqlConfiguration.getMaxIdleTimeSeconds());
        assertEquals(Type.DEFAULT_TYPE, sqlConfiguration.type);
    }

    @Singleton
    public static class DummyBean {
        @Inject
        SqlConfiguration sqlConfiguration;
    }

    @ConfigProperties(prefix = "sql")
    public static class SqlConfiguration {
        @ConfigProperty(name = "max_pool_size")
        public int maxPoolSize = 50;
        @ConfigProperty(name = "max_idle_time_seconds")
        private int maxIdleTimeSeconds = 100;
        public String name;
        public String user;
        private String password;
        public Type type;

        public int getMaxIdleTimeSeconds() {
            return maxIdleTimeSeconds;
        }

        public void setMaxIdleTimeSeconds(int maxIdleTimeSeconds) {
            this.maxIdleTimeSeconds = maxIdleTimeSeconds;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Nested {
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

    public enum Type {
        MSSQL,
        POSTGRES,
        MYSQL,
        DEFAULT_TYPE
    }
}
