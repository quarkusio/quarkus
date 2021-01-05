package io.quarkus.config.yaml.deployment;

import static io.quarkus.config.yaml.runtime.ApplicationYamlProvider.APPLICATION_YAML;
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

public class InterfaceConfigPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBean.class, SqlConfiguration.class, SqlConfiguration.Nested.class,
                            SqlConfiguration.Type.class)
                    .addAsResource("configprops.yaml", APPLICATION_YAML));

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

        assertEquals(2, sqlConfiguration.getInputs().size());
        SqlConfiguration.Nested firstInput = sqlConfiguration.getInputs().get(0);
        assertEquals("my_connection_1", firstInput.name);
        assertEquals("systemuser", firstInput.user);
        assertEquals("secret", firstInput.password);
        assertEquals(SqlConfiguration.Type.MSSQL, firstInput.getType());
        assertEquals(100, firstInput.getMaxPoolSize());
        assertEquals(150, firstInput.maxIdleTimeSeconds);
        SqlConfiguration.Nested secondInput = sqlConfiguration.getInputs().get(1);
        assertEquals("my_connection_2", secondInput.name);
        assertEquals("otheruser", secondInput.user);
        assertEquals("secret", secondInput.password);
        assertEquals(SqlConfiguration.Type.POSTGRES, secondInput.getType());
        assertEquals(10, secondInput.getMaxPoolSize());
        assertEquals(20, secondInput.maxIdleTimeSeconds);

        assertEquals(2, sqlConfiguration.getOutputs().size());
        SqlConfiguration.Nested firstOutput = sqlConfiguration.getOutputs().get(0);
        assertEquals("out_connection_1", firstOutput.name);
        assertEquals("someuser", firstOutput.user);
        assertEquals("asecret", firstOutput.password);
        assertEquals(SqlConfiguration.Type.MSSQL, firstOutput.getType());
        assertEquals(100, firstOutput.getMaxPoolSize());
        assertEquals(200, firstOutput.maxIdleTimeSeconds);
        SqlConfiguration.Nested secondOutput = sqlConfiguration.getOutputs().get(1);
        assertEquals("out_connection_2", secondOutput.name);
        assertEquals("someuser", secondOutput.user);
        assertEquals("asecret", secondOutput.password);
        assertEquals(SqlConfiguration.Type.POSTGRES, secondOutput.getType());
        assertEquals(200, secondOutput.getMaxPoolSize());
        assertEquals(300, secondOutput.maxIdleTimeSeconds);
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

        List<Nested> getInputs();

        List<Nested> getOutputs();

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
