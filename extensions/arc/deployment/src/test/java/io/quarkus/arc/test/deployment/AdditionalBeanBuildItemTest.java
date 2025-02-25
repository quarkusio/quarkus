package io.quarkus.arc.test.deployment;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;

public class AdditionalBeanBuildItemTest {

    @Test
    public void testNullNames() {
        assertThrows(NullPointerException.class, () -> AdditionalBeanBuildItem.unremovableOf((String) null));
        assertThrows(NullPointerException.class, () -> AdditionalBeanBuildItem.unremovableOf((Class<?>) null));
        assertThrows(NullPointerException.class, () -> AdditionalBeanBuildItem.builder().addBeanClasses(null, "Foo").build());
        assertThrows(NullPointerException.class, () -> new AdditionalBeanBuildItem("Bar", null));
        assertThrows(NullPointerException.class, () -> new AdditionalBeanBuildItem(String.class, null));
    }

}
