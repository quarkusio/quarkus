package io.quarkus.extest;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test driver for the test-extension
 */
public class ConfiguredBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfiguredBean.class)
                    .addAsManifestResource("microprofile-config.properties"));

    @Inject
    ConfiguredBean configuredBean;

    /**
     * Simple validation of that the injected bean and root configs are not null
     */
    @Test
    public void validateConfiguredBean() {
        System.out.printf("validateConfiguredBean, %s\n", configuredBean);
        Assertions.assertNotNull(configuredBean);
        Assertions.assertNotNull(configuredBean.getBuildTimeConfig());
        Assertions.assertNotNull(configuredBean.getRunTimeConfig());
    }

    /**
     * Validate that the TestBuildAndRunTimeConfig is the same as seen at build time
     * Currently disabled due to https://github.com/jbossas/quarkus/issues/962
     */
    @Test
    @Disabled("https://github.com/jbossas/quarkus/issues/962")
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
    }

    /**
     * Break out the validation of the RUN_TIME config nested map as that currently is not working.
     */
    @Test
    @Disabled("https://github.com/jbossas/quarkus/issues/956")
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
    }
}
