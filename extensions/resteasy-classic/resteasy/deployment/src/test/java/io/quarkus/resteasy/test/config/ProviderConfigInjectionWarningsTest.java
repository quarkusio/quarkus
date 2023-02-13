package io.quarkus.resteasy.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;

public class ProviderConfigInjectionWarningsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                assertEquals(4, logRecords.size());
                Set<String> messages = logRecords.stream().map(LogRecord::getMessage).collect(Collectors.toSet());
                assertTrue(messages.contains(
                        "Directly injecting a org.eclipse.microprofile.config.Config into a jakarta.ws.rs.ext.Provider may lead to unexpected results. To ensure proper results, please change the type of the field to jakarta.enterprise.inject.Instance<org.eclipse.microprofile.config.Config>. Offending field is 'config' of class 'io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$FooProvider'"));
                assertTrue(messages.contains(
                        "Directly injecting a io.smallrye.config.SmallRyeConfig into a jakarta.ws.rs.ext.Provider may lead to unexpected results. To ensure proper results, please change the type of the field to jakarta.enterprise.inject.Instance<io.smallrye.config.SmallRyeConfig>. Offending field is 'smallRyeConfig' of class 'io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$FooProvider'"));
                assertTrue(messages.contains(
                        "Directly injecting a org.eclipse.microprofile.config.inject.ConfigProperty into a jakarta.ws.rs.ext.Provider may lead to unexpected results. To ensure proper results, please change the type of the field to jakarta.enterprise.inject.Instance<java.lang.String>. Offending field is 'configProperty' of class 'io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$FooProvider'"));
                assertTrue(messages.contains(
                        "Directly injecting a io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$MyConfigMapping into a jakarta.ws.rs.ext.Provider may lead to unexpected results. To ensure proper results, please change the type of the field to jakarta.enterprise.inject.Instance<io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$MyConfigMapping>. Offending field is 'myConfigMapping' of class 'io.quarkus.resteasy.test.config.ProviderConfigInjectionWarningsTest$FooProvider'"));
            });

    @Test
    public void configWarnings() {
        RestAssured.when().get("/test").then().body(Matchers.is("foo"));
    }

    @Path("/test")
    public static class TestResource {
        @Context
        private Providers providers;

        @GET
        public String getFoo() {
            return providers.getContextResolver(String.class, MediaType.TEXT_PLAIN_TYPE).getContext(null);
        }
    }

    @Provider
    public static class FooProvider implements ContextResolver<String> {
        @Inject
        Config config;
        @Inject
        Instance<Config> configInstance;
        @Inject
        SmallRyeConfig smallRyeConfig;
        @Inject
        Instance<SmallRyeConfig> smallRyeConfigInstance;
        @ConfigProperty(name = "configProperty", defaultValue = "configProperty")
        String configProperty;
        @ConfigProperty(name = "configProperty", defaultValue = "configProperty")
        Instance<String> stringInstance;
        @Inject
        MyConfigMapping myConfigMapping;
        @Inject
        Instance<MyConfigMapping> myConfigMappingInstance;

        @Override
        public String getContext(Class<?> type) {
            return "foo";
        }
    }

    @StaticInitSafe
    @ConfigMapping(prefix = "my.mapping")
    public interface MyConfigMapping {
        @WithDefault("myProperty")
        String myProperty();
    }
}
