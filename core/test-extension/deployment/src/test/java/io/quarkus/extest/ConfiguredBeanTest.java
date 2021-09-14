package io.quarkus.extest;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.AnotherPrefixConfig;
import io.quarkus.extest.runtime.config.MyEnum;
import io.quarkus.extest.runtime.config.NestedConfig;
import io.quarkus.extest.runtime.config.ObjectOfValue;
import io.quarkus.extest.runtime.config.ObjectValueOf;
import io.quarkus.extest.runtime.config.OverrideBuildTimeConfigSource;
import io.quarkus.extest.runtime.config.PrefixConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.named.PrefixNamedConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test driver for the test-extension
 */
public class ConfiguredBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfiguredBean.class)
                    // Don't change this to types, because of classloader class cast exception.
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.extest.runtime.config.OverrideBuildTimeConfigSource")
                    .addAsResource("application.properties"));

    @Inject
    Config config;
    @Inject
    ConfiguredBean configuredBean;

    /**
     * Simple validation of that the injected bean and root configs are not null
     */
    @Test
    public void validateConfiguredBean() {
        System.out.printf("validateConfiguredBean, %s%n", configuredBean);
        Assertions.assertNotNull(configuredBean);
        Assertions.assertNotNull(configuredBean.getBuildTimeConfig());
        Assertions.assertNotNull(configuredBean.getRunTimeConfig());
        Assertions.assertEquals("huhu", configuredBean.getFooRuntimeConfig().bar);
    }

    /**
     * Validate that the TestBuildAndRunTimeConfig is the same as seen at build time
     */
    @Test
    public void validateBuildTimeConfig() {
        TestBuildAndRunTimeConfig buildTimeConfig = configuredBean.getBuildTimeConfig();
        Assertions.assertEquals("StringBasedValue", buildTimeConfig.btSBV.getValue(),
                "buildTimeConfig.btSBV != StringBasedValue; " + buildTimeConfig.btSBV.getValue());
        Assertions.assertEquals("btSBVWithDefaultValue", buildTimeConfig.btSBVWithDefault.getValue(),
                "buildTimeConfig.btSBVWithDefault != btSBVWithDefaultValue; " + buildTimeConfig.btSBVWithDefault.getValue());
        Assertions.assertEquals("btStringOptValue", buildTimeConfig.btStringOpt,
                "buildTimeConfig.btStringOpt != btStringOptValue; " + buildTimeConfig.btStringOpt);

        // quarkus.btrt.all-values.double-primitive=3.1415926535897932384
        Assertions.assertEquals(3.1415926535897932384, buildTimeConfig.allValues.doublePrimitive, 0.00000001);
        // quarkus.btrt.all-values.opt-double-value=3.1415926535897932384
        Assertions.assertTrue(buildTimeConfig.allValues.optDoubleValue.isPresent(),
                "runTimeConfig.allValues.optDoubleValue.isPresent");
        Assertions.assertEquals(3.1415926535897932384, buildTimeConfig.allValues.optDoubleValue.getAsDouble(), 0.00000001);

        if (!buildTimeConfig.btStringOptWithDefault.equals("btStringOptWithDefaultValue")) {
            throw new IllegalStateException("buildTimeConfig.btStringOptWithDefault != btStringOptWithDefaultValue; "
                    + buildTimeConfig.btStringOptWithDefault);
        }
        if (!buildTimeConfig.allValues.oov.equals(new ObjectOfValue("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; " + buildTimeConfig.allValues.oov);
        }
        if (!buildTimeConfig.allValues.oovWithDefault.equals(new ObjectOfValue("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException(
                    "buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; " + buildTimeConfig.allValues.oovWithDefault);
        }
        if (!buildTimeConfig.allValues.ovo.equals(new ObjectValueOf("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; " + buildTimeConfig.allValues.oov);
        }
        if (!buildTimeConfig.allValues.ovoWithDefault.equals(new ObjectValueOf("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException(
                    "buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; " + buildTimeConfig.allValues.oovWithDefault);
        }
        if (buildTimeConfig.allValues.longPrimitive != 1234567891L) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longPrimitive != 1234567891L; " + buildTimeConfig.allValues.longPrimitive);
        }
        if (buildTimeConfig.allValues.longValue != 1234567892L) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longValue != 1234567892L; " + buildTimeConfig.allValues.longValue);
        }
        if (buildTimeConfig.allValues.optLongValue.getAsLong() != 1234567893L) {
            throw new IllegalStateException(
                    "buildTimeConfig.optLongValue != 1234567893L; " + buildTimeConfig.allValues.optLongValue.getAsLong());
        }
        if (buildTimeConfig.allValues.optionalLongValue.get() != 1234567894L) {
            throw new IllegalStateException("buildTimeConfig.allValues.optionalLongValue != 1234567894L; "
                    + buildTimeConfig.allValues.optionalLongValue.get());
        }
        if (buildTimeConfig.allValues.nestedConfigMap.size() != 2) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.simpleMap.size != 2; " + buildTimeConfig.allValues.nestedConfigMap.size());
        }
        Assertions.assertNotEquals("${java.vm.version}", buildTimeConfig.allValues.expandedDefault);
        Assertions.assertFalse(buildTimeConfig.allValues.expandedDefault.isEmpty());
        List<MyEnum> enums = Arrays.asList(MyEnum.OPTIONAL, MyEnum.ENUM_ONE, MyEnum.Enum_Two);
        Assertions.assertEquals(enums, configuredBean.getBuildTimeConfig().myEnums);
        Assertions.assertEquals(MyEnum.OPTIONAL, configuredBean.getBuildTimeConfig().myEnum);
        List<Integer> mapValues = new ArrayList<>(Arrays.asList(1, 2));
        List<Integer> actualMapValues = new ArrayList<>(configuredBean.getBuildTimeConfig().mapOfNumbers.values());
        Assertions.assertEquals(mapValues, actualMapValues);

    }

    /**
     * Validate the TestRunTimeConfig up to
     */
    @Test
    public void validateRuntimeConfig() {
        TestRunTimeConfig runTimeConfig = configuredBean.getRunTimeConfig();
        // quarkus.rt.rt-string-opt=rtStringOptValue
        Assertions.assertEquals("rtStringOptValue", runTimeConfig.rtStringOpt);
        // quarkus.rt.rt-string-opt-with-default=rtStringOptWithDefaultValue
        Assertions.assertEquals("rtStringOptWithDefaultValue", runTimeConfig.rtStringOptWithDefault);

        //quarkus.rt.all-values.oov=configPart1+configPart2
        Assertions.assertEquals(new ObjectOfValue("configPart1", "configPart2"), runTimeConfig.allValues.oov);
        //quarkus.rt.all-values.ovo=configPart1+configPart2
        Assertions.assertEquals(new ObjectValueOf("configPart1", "configPart2"), runTimeConfig.allValues.ovo);
        // quarkus.rt.all-values.long-primitive=12345678911
        Assertions.assertEquals(12345678911l, runTimeConfig.allValues.longPrimitive);
        // quarkus.rt.all-values.long-value=12345678921
        Assertions.assertEquals(12345678921l, runTimeConfig.allValues.longValue.longValue());
        // quarkus.rt.all-values.opt-long-value=12345678931
        Assertions.assertTrue(runTimeConfig.allValues.optLongValue.isPresent(),
                "runTimeConfig.allValues.optLongValue.isPresent");
        Assertions.assertEquals(12345678931l, runTimeConfig.allValues.optLongValue.getAsLong());
        // quarkus.rt.all-values.optional-long-value=12345678941
        Assertions.assertTrue(runTimeConfig.allValues.optionalLongValue.isPresent(),
                "runTimeConfig.allValues.optionalLongValue.isPresent");
        Assertions.assertEquals(12345678941l, runTimeConfig.allValues.optionalLongValue.get().longValue());
        // quarkus.btrt.all-values.double-primitive=3.1415926535897932384
        Assertions.assertEquals(3.1415926535897932384, runTimeConfig.allValues.doublePrimitive, 0.00000001);
        // quarkus.btrt.all-values.opt-double-value=3.1415926535897932384
        Assertions.assertTrue(runTimeConfig.allValues.optDoubleValue.isPresent(),
                "runTimeConfig.allValues.optDoubleValue.isPresent");
        Assertions.assertEquals(3.1415926535897932384, runTimeConfig.allValues.optDoubleValue.getAsDouble(), 0.00000001);
        // quarkus.rt.all-values.string-list=value1,value2
        Assertions.assertEquals(2, runTimeConfig.allValues.stringList.size());
        Assertions.assertEquals("value1", runTimeConfig.allValues.stringList.get(0));
        Assertions.assertEquals("value2", runTimeConfig.allValues.stringList.get(1));
        // quarkus.rt.all-values.long-list=1,2,3
        Assertions.assertEquals(3, runTimeConfig.allValues.longList.size());
        Assertions.assertEquals(1, runTimeConfig.allValues.longList.get(0).longValue());
        Assertions.assertEquals(2, runTimeConfig.allValues.longList.get(1).longValue());
        Assertions.assertEquals(3, runTimeConfig.allValues.longList.get(2).longValue());
        Assertions.assertNotEquals("${java.vm.version}", runTimeConfig.allValues.expandedDefault);
        Assertions.assertFalse(runTimeConfig.allValues.expandedDefault.isEmpty());
    }

    /**
     * Break out the validation of the RUN_TIME config nested map as that currently is not working.
     */
    @Test
    public void validateRuntimeConfigMap() {
        TestRunTimeConfig runTimeConfig = configuredBean.getRunTimeConfig();
        Assertions.assertEquals(2, runTimeConfig.allValues.nestedConfigMap.size());
        //quarkus.rt.all-values.nested-config-map.key1.nested-value=value1
        //quarkus.rt.all-values.nested-config-map.key1.oov=value1.1+value1.2
        NestedConfig nc1 = runTimeConfig.allValues.nestedConfigMap.get("key1");
        Assertions.assertNotNull(nc1);
        Assertions.assertEquals("value1", nc1.nestedValue);
        Assertions.assertEquals(new ObjectOfValue("value1.1", "value1.2"), nc1.oov);
        //quarkus.rt.all-values.nested-config-map.key2.nested-value=value2
        //quarkus.rt.all-values.nested-config-map.key2.oov=value2.1+value2.2
        NestedConfig nc2 = runTimeConfig.allValues.nestedConfigMap.get("key2");
        Assertions.assertNotNull(nc2);
        Assertions.assertEquals("value2", nc2.nestedValue);
        Assertions.assertEquals(new ObjectOfValue("value2.1", "value2.2"), nc2.oov);
        //quarkus.rt.all-values.string-map.key1=value1
        //quarkus.rt.all-values.string-map.key2=value2
        //quarkus.rt.all-values.string-map.key3=value3
        final Map<String, String> stringMap = runTimeConfig.allValues.stringMap;
        Assertions.assertEquals("value1", stringMap.get("key1"));
        Assertions.assertEquals("value2", stringMap.get("key2"));
        Assertions.assertEquals("value3", stringMap.get("key3"));
        //quarkus.rt.all-values.string-list-map.key1=value1,value2,value3
        //quarkus.rt.all-values.string-list-map.key2=value4,value5
        //quarkus.rt.all-values.string-list-map.key3=value6
        final Map<String, List<String>> stringListMap = runTimeConfig.allValues.stringListMap;
        Assertions.assertEquals(Arrays.asList("value1", "value2", "value3"), stringListMap.get("key1"));
        Assertions.assertEquals(Arrays.asList("value4", "value5"), stringListMap.get("key2"));
        Assertions.assertEquals(Collections.singletonList("value6"), stringListMap.get("key3"));

        //quarkus.rt.leaf-map.key.first=first-key-value
        //quarkus.rt.leaf-map.key.second=second-key-value

        final Map<String, Map<String, String>> leafMap = runTimeConfig.leafMap;
        Assertions.assertEquals("first-key-value", leafMap.get("key").get("first"));
        Assertions.assertEquals("second-key-value", leafMap.get("key").get("second"));

        //quarkus.rt.config-group-map.key.group.nested-value=value
        //quarkus.rt.config-group-map.key.group.oov=value2.1+value2.2
        final Map<String, Map<String, NestedConfig>> configGroupMap = runTimeConfig.configGroupMap;
        NestedConfig nestedConfigFromMap = configGroupMap.get("key").get("group");
        Assertions.assertEquals("value", nestedConfigFromMap.nestedValue);
        Assertions.assertEquals(new ObjectOfValue("value2.1", "value2.2"), nestedConfigFromMap.oov);
    }

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
    public void testHyphenatedEnumConversion() {
        List<MyEnum> enums = Arrays.asList(MyEnum.ENUM_ONE, MyEnum.Enum_Two);
        Assertions.assertEquals(enums, configuredBean.getRunTimeConfig().myEnums);
        Assertions.assertEquals(MyEnum.Enum_Two, configuredBean.getRunTimeConfig().myEnum);
        Assertions.assertEquals(MyEnum.OPTIONAL, configuredBean.getRunTimeConfig().myOptionalEnums.get());
        Assertions.assertEquals(MyEnum.ENUM_ONE, configuredBean.getRunTimeConfig().noHyphenateFirstEnum.get());
        Assertions.assertEquals(MyEnum.Enum_Two, configuredBean.getRunTimeConfig().noHyphenateSecondEnum.get());
    }

    @Test
    public void testConversionUsingConvertWith() {
        Assertions.assertTrue(configuredBean.getRunTimeConfig().primitiveBoolean);
        Assertions.assertFalse(configuredBean.getRunTimeConfig().objectBoolean);
        Assertions.assertEquals(2, configuredBean.getRunTimeConfig().primitiveInteger);
        Assertions.assertEquals(9, configuredBean.getRunTimeConfig().objectInteger);
        List<Integer> oneToNine = IntStream.range(1, 10).mapToObj(Integer::valueOf).collect(Collectors.toList());
        Assertions.assertEquals(oneToNine, configuredBean.getRunTimeConfig().oneToNine);
        List<Integer> mapValues = new ArrayList<>(Arrays.asList(1, 2));
        List<Integer> actualMapValues = new ArrayList<>(configuredBean.getRunTimeConfig().mapOfNumbers.values());
        Assertions.assertEquals(mapValues, actualMapValues);
    }

    @Test
    public void testBtrtMapOfMap() {
        Map<String, Map<String, String>> mapMap = configuredBean.getBuildTimeConfig().mapMap;
        Assertions.assertFalse(mapMap.containsKey("inner-key"));
        Assertions.assertTrue(mapMap.containsKey("outer-key"));
        Map<String, String> map = mapMap.get("outer-key");
        Assertions.assertTrue(map.containsKey("inner-key"));
        Assertions.assertFalse(map.containsKey("outer-key"));
        Assertions.assertEquals("1234", map.get("inner-key"));
    }

    @Test
    public void testRtMapOfMap() {
        Map<String, Map<String, String>> mapMap = configuredBean.getRunTimeConfig().mapMap;
        Assertions.assertFalse(mapMap.containsKey("inner-key"));
        Assertions.assertTrue(mapMap.containsKey("outer-key"));
        Map<String, String> map = mapMap.get("outer-key");
        Assertions.assertTrue(map.containsKey("inner-key"));
        Assertions.assertFalse(map.containsKey("outer-key"));
        Assertions.assertEquals("1234", map.get("inner-key"));
    }

    @Inject
    TestBuildAndRunTimeConfig buildAndRunTimeConfig;

    @Test
    public void buildTimeDefaults() {
        // Source is only initialized once in runtime.
        Assertions.assertEquals(1, OverrideBuildTimeConfigSource.counter.get());
        // Test that build configRoot are not overridden by properties in runtime.
        Assertions.assertEquals(1234567891L, buildAndRunTimeConfig.allValues.longPrimitive);
        Assertions.assertEquals(0, ConfigProvider.getConfig().getValue("quarkus.btrt.all-values.long-primitive", Long.class));
    }

    @Test
    public void testBuiltTimeNamedMapWithProfiles() {
        Map<String, Map<String, String>> mapMap = configuredBean.getBuildTimeConfig().mapMap;
        Assertions.assertEquals("1234", mapMap.get("main-profile").get("property"));
        Assertions.assertEquals("5678", mapMap.get("test-profile").get("property"));
    }

    @Test
    public void testConfigDefaultValuesSourceOrdinal() {
        ConfigSource defaultValues = null;
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("PropertiesConfigSource[source=Specified default values]")) {
                defaultValues = configSource;
                break;
            }
        }
        assertNotNull(defaultValues);
        assertEquals(Integer.MIN_VALUE + 100, defaultValues.getOrdinal());

        // Should be the first
        ConfigSource applicationProperties = config.getConfigSources().iterator().next();
        assertNotNull(applicationProperties);
        assertEquals(1000, applicationProperties.getOrdinal());

        assertEquals("1234", defaultValues.getValue("my.prop"));
        assertEquals("1234", applicationProperties.getValue("my.prop"));
    }

    @Test
    public void testProfileDefaultValuesSource() {
        ConfigSource defaultValues = null;
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("PropertiesConfigSource[source=Specified default values]")) {
                defaultValues = configSource;
                break;
            }
        }
        assertNotNull(defaultValues);
        assertEquals("1234", defaultValues.getValue("my.prop"));
        assertEquals("1234", defaultValues.getValue("%prod.my.prop"));
        assertEquals("5678", defaultValues.getValue("%dev.my.prop"));
        assertEquals("1234", defaultValues.getValue("%test.my.prop"));
        assertEquals("1234", config.getValue("my.prop", String.class));
    }

    @Test
    void prefixConfig() {
        PrefixConfig prefixConfig = configuredBean.getPrefixConfig();
        assertNotNull(prefixConfig);
        assertEquals("1234", prefixConfig.prop);
        assertEquals("1234", prefixConfig.map.get("prop"));
        assertEquals("nested-1234", prefixConfig.nested.nestedValue);
        assertEquals("nested-1234", prefixConfig.nested.oov.getPart1());
        assertEquals("nested-5678", prefixConfig.nested.oov.getPart2());

        PrefixNamedConfig prefixNamedConfig = configuredBean.getPrefixNamedConfig();
        assertNotNull(prefixNamedConfig);
        assertEquals("1234", prefixNamedConfig.prop);
        assertEquals("1234", prefixNamedConfig.map.get("prop"));
        assertEquals("nested-1234", prefixNamedConfig.nested.nestedValue);
        assertEquals("nested-1234", prefixNamedConfig.nested.oov.getPart1());
        assertEquals("nested-5678", prefixNamedConfig.nested.oov.getPart2());

        AnotherPrefixConfig anotherPrefixConfig = configuredBean.getAnotherPrefixConfig();
        assertNotNull(anotherPrefixConfig);
        assertEquals("5678", anotherPrefixConfig.prop);
        assertEquals("5678", anotherPrefixConfig.map.get("prop"));

        ConfigSource defaultValues = null;
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("PropertiesConfigSource[source=Specified default values]")) {
                defaultValues = configSource;
                break;
            }
        }
        assertNotNull(defaultValues);
        // java.version should not be recorded
        assertFalse(defaultValues.getPropertyNames().contains("java.version"));
    }
}
