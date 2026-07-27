package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.ConfigValidationException;
import tools.jackson.databind.ObjectMapper;

public class JacksonErroneousTimeZonePropertiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Pojo.class, SomeBean.class))
            .withConfigurationResource("application-erroneous-timezone-properties.properties")
            .setExpectedException(ConfigValidationException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class SomeBean {

        @Inject
        ObjectMapper objectMapper;

        public String write(Pojo pojo) {
            return objectMapper.writeValueAsString(pojo);
        }

    }

    public static class Pojo {

        private final Date date;

        public Pojo(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }

}
