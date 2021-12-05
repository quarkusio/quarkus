package io.quarkus.jackson.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import java.time.zone.ZoneRulesException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonErroneousTimeZonePropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Pojo.class, SomeBean.class))
            .withConfigurationResource("application-erroneous-timezone-properties.properties")
            .setExpectedException(ZoneRulesException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class SomeBean {

        @Inject
        ObjectMapper objectMapper;

        public String write(Pojo pojo) throws JsonProcessingException {
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
