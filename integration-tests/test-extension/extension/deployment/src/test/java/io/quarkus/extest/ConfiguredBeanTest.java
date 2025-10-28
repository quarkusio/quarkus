package io.quarkus.extest;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.DoNotRecordEnvConfigSource;
import io.quarkus.extest.runtime.config.TestMappingBuildTimeRunTime;
import io.quarkus.extest.runtime.config.TestShadowBuildTimeToRunTimeConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

/**
 * Test driver for the test-extension
 */
public class ConfiguredBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsServiceProvider(ConfigSource.class, DoNotRecordEnvConfigSource.class)
                    .addAsResource("application.properties"));

    @Inject
    SmallRyeConfig config;
    @Inject
    TestMappingBuildTimeRunTime buildAndRunTimeConfig;
    @Inject
    TestShadowBuildTimeToRunTimeConfig shadowBuildTimeToRunTimeConfig;

    /**
     * Test the RuntimeXmlConfigService using old school sockets
     */
    @Test
    public void testRuntimeXmlConfigService() throws Exception {
        // From config.xml
        Socket socket = new Socket("localhost", 12345);
        OutputStream os = socket.getOutputStream();
        os.write("testRuntimeXmlConfigService\n".getBytes("UTF-8"));
        os.flush();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String reply = reader.readLine();
            Assertions.assertEquals("testRuntimeXmlConfigService-ack", reply);
        } finally {
            os.close();
            socket.close();
        }
    }

    @Test
    public void verifyCommandServlet() {
        RestAssured.when().get("/commands/ping").then()
                .body(is("/ping-ack"));
    }

    @Test
    public void buildTimeDefaults() {
        // Test that build mapping is not overridden by properties set in static or runtime init
        Assertions.assertEquals("value", buildAndRunTimeConfig.value());

        ConfigValue value = config.getConfigValue("quarkus.mapping.btrt.value");
        Assertions.assertEquals("value", value.getValue());
        Assertions.assertEquals("BuildTime RunTime Fixed", value.getConfigSourceName());
        Assertions.assertEquals(Integer.MAX_VALUE, value.getConfigSourceOrdinal());
    }

    @Test
    public void testBuiltTimeNamedMapWithProfiles() {
        Map<String, Map<String, String>> mapMap = buildAndRunTimeConfig.mapMap();
        Assertions.assertEquals("1234", mapMap.get("main-profile").get("property"));
        Assertions.assertEquals("5678", mapMap.get("test-profile").get("property"));
    }

    @Test
    public void testConfigRuntimeValuesSourceOrdinal() {
        Optional<ConfigSource> source = config.getConfigSource("Runtime Values");
        assertTrue(source.isPresent());
        ConfigSource runtimeValues = source.get();
        assertEquals(0, runtimeValues.getOrdinal());

        ConfigSource applicationProperties = null;
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("application.properties")) {
                applicationProperties = configSource;
                break;
            }
        }
        assertNotNull(applicationProperties);
        assertEquals(1000, applicationProperties.getOrdinal());

        assertEquals("9999", runtimeValues.getValue("%test.my.prop"));
        assertEquals("9999", applicationProperties.getValue("%test.my.prop"));
        assertEquals("1234", runtimeValues.getValue("my.prop"));
        assertEquals("1234", applicationProperties.getValue("my.prop"));

        assertEquals("9999", config.getConfigValue("my.prop").getValue());
    }

    @Test
    public void testProfileRuntimeValuesSource() {
        Optional<ConfigSource> source = config.getConfigSource("Runtime Values");
        assertTrue(source.isPresent());
        ConfigSource runtimeValues = source.get();

        assertEquals("1234", runtimeValues.getValue("%prod.my.prop"));
        assertEquals("5678", runtimeValues.getValue("%dev.my.prop"));
        assertEquals("9999", runtimeValues.getValue("%test.my.prop"));
        // this runs with the test profile
        assertEquals("9999", config.getValue("my.prop", String.class));

        // runtime properties coming from env must not be recorded
        assertNull(runtimeValues.getValue("should.not.be.recorded"));
        assertNull(runtimeValues.getValue("SHOULD_NOT_BE_RECORDED"));
        assertNull(runtimeValues.getValue("quarkus.mapping.rt.do-not-record"));
        assertNull(runtimeValues.getValue("%prod.quarkus.mapping.rt.do-not-record"));
        assertNull(runtimeValues.getValue("%dev.quarkus.mapping.rt.do-not-record"));
        assertEquals("value", config.getConfigValue("quarkus.mapping.rt.do-not-record").getValue());
    }

    @Test
    void shadowBuildTimeToRunTimeConfig() {
        ConfigValue btConfigValue = shadowBuildTimeToRunTimeConfig.btConfigValue();

        assertEquals("quarkus.bt.bt-config-value", btConfigValue.getName());
        assertEquals("value", btConfigValue.getValue());

        Optional<ConfigSource> source = config.getConfigSource("Runtime Values");
        assertTrue(source.isPresent());
        ConfigSource runtimeValues = source.get();

        assertTrue(runtimeValues.getPropertyNames().contains("quarkus.bt.bt-config-value"));
        assertEquals("${test.record.expansion}", runtimeValues.getValue("quarkus.bt.bt-config-value"));
    }
}
