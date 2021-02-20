package io.quarkus.config.yaml.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.test.QuarkusUnitTest;

public class ClassConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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

        assertEquals(2, sqlConfiguration.inputs.size());
        Nested firstInput = sqlConfiguration.inputs.get(0);
        assertEquals("my_connection_1", firstInput.name);
        assertEquals("systemuser", firstInput.user);
        assertEquals("secret", firstInput.password);
        assertEquals(Type.MSSQL, firstInput.getType());
        assertEquals(100, firstInput.getMaxPoolSize());
        assertEquals(150, firstInput.maxIdleTimeSeconds);
        Nested secondInput = sqlConfiguration.inputs.get(1);
        assertEquals("my_connection_2", secondInput.name);
        assertEquals("otheruser", secondInput.user);
        assertEquals("secret", secondInput.password);
        assertEquals(Type.POSTGRES, secondInput.getType());
        assertEquals(10, secondInput.getMaxPoolSize());
        assertEquals(20, secondInput.maxIdleTimeSeconds);

        assertEquals(2, sqlConfiguration.getOutputs().size());
        Nested firstOutput = sqlConfiguration.getOutputs().get(0);
        assertEquals("out_connection_1", firstOutput.name);
        assertEquals("someuser", firstOutput.user);
        assertEquals("asecret", firstOutput.password);
        assertEquals(Type.MSSQL, firstOutput.getType());
        assertEquals(100, firstOutput.getMaxPoolSize());
        assertEquals(200, firstOutput.maxIdleTimeSeconds);
        Nested secondOutput = sqlConfiguration.getOutputs().get(1);
        assertEquals("out_connection_2", secondOutput.name);
        assertEquals("someuser", secondOutput.user);
        assertEquals("asecret", secondOutput.password);
        assertEquals(Type.POSTGRES, secondOutput.getType());
        assertEquals(200, secondOutput.getMaxPoolSize());
        assertEquals(300, secondOutput.maxIdleTimeSeconds);
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
        public List<Nested> inputs;
        private List<Nested> outputs;

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

        public List<Nested> getOutputs() {
            return outputs;
        }

        public void setOutputs(List<Nested> outputs) {
            this.outputs = outputs;
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
