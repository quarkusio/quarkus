package io.quarkus.devservices.crossclassloader.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigInstanceBuilder;

// Be aware that many challenges with ComparableDevServicesConfigTest come from operating across classloaders, and this test does not exercise that
// The other challenges come from working with proxied annotations, and this test also doesn't exercise that. :)
public class ComparableDevServicesConfigTest {

    String configName = "prefix";
    UUID uuid = UUID.randomUUID();

    @Test
    public void identicalWrappersShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        CI globalConfig = config(3);
        CI config = config(1);
        ComparableDevServicesConfig wrapped = new ComparableDevServicesConfig(uuid, owner, globalConfig, config);
        assertEquals(wrapped, wrapped);
    }

    @Test
    public void wrappersWrappingIdenticalObjectsShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        CI globalConfig = config(3);
        CI config = config(1);
        assertEquals(new ComparableDevServicesConfig(uuid, owner, globalConfig, config),
                new ComparableDevServicesConfig(uuid, owner, globalConfig, config));
    }

    @Test
    public void wrappersWrappingEquivalentObjectsShouldBeEqual() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        CI globalConfig1 = config(3);
        CI config1 = config(1);
        CI globalConfig2 = config(3);
        CI config2 = config(1);
        assertEquals(new ComparableDevServicesConfig(uuid, owner, globalConfig1, config1),
                new ComparableDevServicesConfig(uuid, owner, globalConfig2, config2));
    }

    @Test
    public void wrappersWrappingIdenticalObjectsShouldBeHaveTheSameHashCode() {
        DevServiceOwner owner = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        CI globalConfig = config(3);
        CI config = config(1);
        assertEquals(new ComparableDevServicesConfig(uuid, owner, globalConfig, config).hashCode(),
                new ComparableDevServicesConfig(uuid, owner, globalConfig, config).hashCode());
    }

    @Test
    public void wrappersWrappingDifferentOwnerExtensionsShouldNotBeEqual() {
        DevServiceOwner owner1 = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DevServiceOwner owner2 = new DevServiceOwner("anotherextension", LaunchMode.TEST.name(), configName);
        assertNotEquals(new ComparableDevServicesConfig(uuid, owner1, null, null),
                new ComparableDevServicesConfig(uuid, owner2, null, null));
    }

    @Test
    public void wrappersWrappingDifferentOwnerLaunchModesShouldNotBeEqual() {
        DevServiceOwner owner1 = new DevServiceOwner("someextension", LaunchMode.TEST.name(), configName);
        DevServiceOwner owner2 = new DevServiceOwner("someextension", LaunchMode.DEVELOPMENT.name(), configName);
        assertNotEquals(new ComparableDevServicesConfig(uuid, owner1, null, null),
                new ComparableDevServicesConfig(uuid, owner2, null, null));
    }

    @Test
    public void wrappersWrappingDifferentIdentifyingConfigShouldNotBeEqual() {
        CI config1 = config(1);
        CI config2 = config(2);
        assertNotEquals(new ComparableDevServicesConfig(uuid, null, null, config1),
                new ComparableDevServicesConfig(uuid, null, null, config2));
    }

    @Test
    public void wrappersWrappingDifferentIdentifyingConfigHaveDifferentHashCodes() {
        CI config1 = config(1);
        CI config2 = config(2);
        assertNotEquals(new ComparableDevServicesConfig(uuid, null, null, config1).hashCode(),
                new ComparableDevServicesConfig(uuid, null, null, config2).hashCode());
    }

    @Test
    public void nullUuidIsHandled() {
        CI config = config(1);
        assertNotNull(new ComparableDevServicesConfig(null, null, null, config).hashCode());
        assertEquals(new ComparableDevServicesConfig(null, null, null, config).hashCode(),
                new ComparableDevServicesConfig(null, null, null, config).hashCode());
    }

    @Test
    public void sameConstantsAreEqual() {
        var a1 = new ComparableDevServicesConfig(uuid, null, null, "1");
        var a2 = new ComparableDevServicesConfig(uuid, null, null, "1");
        assertEquals(a1, a2);
    }

    @Test
    public void differentConstantsAreNotEqual() {
        var a1 = new ComparableDevServicesConfig(uuid, null, null, "2");
        var a2 = new ComparableDevServicesConfig(uuid, null, null, "1");
        assertNotEquals(a1, a2);
    }

    @ConfigGroup
    interface CI {

        int a();

    }

    private static CI config(int a) {
        return ConfigInstanceBuilder.forInterface(CI.class).with(CI::a, a).build();
    }
}
