package io.quarkus.jackson.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JacksonMapperBuilderTypeTestBase {

    @Inject
    ObjectMapper objectMapper;

    abstract ObjectMapper explicitObjectMapper();

    @Test
    public void testType() throws Exception {
        byte[] bytes = objectMapper.writeValueAsBytes(new Pojo("foobar"));
        ObjectMapper om = explicitObjectMapper();
        Pojo pojo = om.readValue(bytes, Pojo.class);
        Assertions.assertEquals("foobar", pojo.getS());
    }

    public static class Pojo {
        private String s;

        public Pojo() {
        }

        public Pojo(String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }

}
