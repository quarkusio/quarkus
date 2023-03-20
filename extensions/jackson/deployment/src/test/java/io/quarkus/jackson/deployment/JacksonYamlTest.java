package io.quarkus.jackson.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonYamlTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Config.class));

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testYaml() throws Exception {
        byte[] bytes = objectMapper.writeValueAsBytes(new Pojo("foobar"));
        YAMLMapper ym = new YAMLMapper();
        Pojo pojo = ym.readValue(bytes, Pojo.class);
        Assertions.assertEquals("foobar", pojo.getS());
    }

    @ApplicationScoped
    public static class Config {
        @SuppressWarnings("rawtypes")
        @Singleton
        @Produces
        public MapperBuilder yamlBuilder() {
            return YAMLMapper.builder();
        }
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
