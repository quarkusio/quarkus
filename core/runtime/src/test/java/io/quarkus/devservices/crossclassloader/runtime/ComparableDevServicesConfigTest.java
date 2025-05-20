package io.quarkus.devservices.crossclassloader.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigGroup;

// Be aware that many challenges with ComparableDevServicesConfigTest come from operating across classloaders, and this test does not exercise that
// The other challenges come from working with proxied annotations, and this test also doesn't exercise that. :)
public class ComparableDevServicesConfigTest {

    String configName = "prefix";

    @Test
    public void identicalWrappersShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DummyDevServicesConfig globalConfig = new DummyDevServicesConfig("b", 3);
        DummyExtensionConfig config = new DummyExtensionConfig("a", 1);
        ComparableDevServicesConfig wrapped = new ComparableDevServicesConfig(owner, globalConfig, config);
        assertEquals(wrapped, wrapped);
    }

    @Test
    public void wrappersWrappingIdenticalObjectsShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DummyDevServicesConfig globalConfig = new DummyDevServicesConfig("b", 3);
        DummyExtensionConfig config = new DummyExtensionConfig("a", 1);
        assertEquals(new ComparableDevServicesConfig(owner, globalConfig, config),
                new ComparableDevServicesConfig(owner, globalConfig, config));
    }

    @Test
    public void wrappersWrappingEquivalentObjectsShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DummyDevServicesConfig globalConfig1 = new DummyDevServicesConfig("b", 3);
        DummyExtensionConfig config1 = new DummyExtensionConfig("a", 1);
        DummyDevServicesConfig globalConfig2 = new DummyDevServicesConfig("b", 3);
        DummyExtensionConfig config2 = new DummyExtensionConfig("a", 1);
        assertEquals(new ComparableDevServicesConfig(owner, globalConfig1, config1),
                new ComparableDevServicesConfig(owner, globalConfig2, config2));
    }

    @Test
    public void wrappersWrappingIdenticalObjectsShouldBeHaveTheSameHashCode() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DummyDevServicesConfig globalConfig = new DummyDevServicesConfig("b", 3);
        DummyExtensionConfig config = new DummyExtensionConfig("a", 1);
        assertEquals(new ComparableDevServicesConfig(owner, globalConfig, config).hashCode(),
                new ComparableDevServicesConfig(owner, globalConfig, config).hashCode());
    }

    @Test
    public void wrappersWrappingDifferentOwnerExtensionsShouldNotBeEqual() {
        DevServiceOwner owner1 = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DevServiceOwner owner2 = new DevServiceOwner("anotherextension", LaunchMode.TEST.name(), configName);
        assertNotEquals(new ComparableDevServicesConfig(owner1, null, null),
                new ComparableDevServicesConfig(owner2, null, null));
    }

    @Test
    public void wrappersWrappingDifferentOwnerLaunchModesShouldNotBeEqual() {
        DevServiceOwner owner1 = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DevServiceOwner owner2 = new DevServiceOwner("someextension", LaunchMode.DEVELOPMENT.name(), configName);
        assertNotEquals(new ComparableDevServicesConfig(owner1, null, null),
                new ComparableDevServicesConfig(owner2, null, null));
    }

    @Test
    public void wrappersWrappingDifferentIdentifyingConfigShouldNotBeEqual() {
        DummyExtensionConfig config1 = new DummyExtensionConfig("a", 1);
        DummyExtensionConfig config2 = new DummyExtensionConfig("a", 2);
        assertNotEquals(new ComparableDevServicesConfig(null, null, config1),
                new ComparableDevServicesConfig(null, null, config2));
    }

    @Test
    public void wrappersWrappingDifferentIdentifyingConfigHaveDifferentHashCodes() {
        DummyExtensionConfig config1 = new DummyExtensionConfig("a", 1);
        DummyExtensionConfig config2 = new DummyExtensionConfig("a", 2);
        assertNotEquals(new ComparableDevServicesConfig(null, null, config1).hashCode(),
                new ComparableDevServicesConfig(null, null, config2).hashCode());
    }

    @ConfigGroup
    interface CI {

        int a();

    }

    private class DummyDevServicesConfig implements CI {
        String s;
        int i;

        public DummyDevServicesConfig(String a, int i) {
            this.s = a;
            this.i = i;
        }

        @Override
        public int a() {
            return i;
        }
    }

    private class DummyExtensionConfig implements CI {
        String s;
        int i;

        public DummyExtensionConfig(String a, int i) {
            this.s = a;
            this.i = i;
        }

        @Override
        public int a() {
            return i;
        }
    }
}