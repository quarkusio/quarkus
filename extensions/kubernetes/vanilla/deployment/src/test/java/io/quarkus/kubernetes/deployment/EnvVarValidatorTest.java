package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

class EnvVarValidatorTest {

    private static final String TARGET = "kubernetes";
    private EnvVarValidator validator;

    @BeforeEach
    void init() {
        this.validator = new EnvVarValidator();
    }

    @Test
    void getBuildItemsShouldReturnEmptyOnNoItems() {
        assertTrue(validator.getBuildItems().isEmpty());
    }

    @Test
    void getBuildItemsOneItemShouldWork() {
        final KubernetesEnvBuildItem initial = KubernetesEnvBuildItem.createSimpleVar("name", "value", TARGET);
        validator.process(initial);
        final Collection<KubernetesEnvBuildItem> items = validator.getBuildItems();
        assertEquals(1, items.size());
        assertEquals(initial, items.stream().findFirst().orElseGet(() -> fail("no item was found when one was expected")));
    }

    @Test
    void getBuildItemsTwoConflictingItemsShouldFail() {
        final String name = "name";
        final String value1 = "foo";
        final String value2 = "bar";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createSimpleVar(name, value1, TARGET);
        final KubernetesEnvBuildItem second = KubernetesEnvBuildItem.createFromField(name, value2, TARGET);
        validator.process(first);
        validator.process(second);
        try {
            validator.getBuildItems();
            fail();
        } catch (Exception e) {
            final String message = e.getMessage();
            assertTrue(message.contains(name) && message.contains(value1) && message.contains(value2));
        }
    }

    @Test
    void getBuildItemsTwoRedundantItemsShouldResultInOnlyOneItem() {
        final String name = "name";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createFromConfigMap(name, TARGET, null);
        final KubernetesEnvBuildItem second = KubernetesEnvBuildItem.createFromConfigMap(name, TARGET, null);
        validator.process(first);
        validator.process(second);
        final Collection<KubernetesEnvBuildItem> items = validator.getBuildItems();
        assertEquals(1, items.size());
        assertEquals(first, items.stream().findFirst().orElseGet(() -> fail("no item was found when one was expected")));
    }

    @Test
    void getBuildItemsSimilarlyNamedCompatibleItemsShouldWork() {
        final String name = "name";
        final String value1 = "foo";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createSimpleVar(name, value1, TARGET);
        final KubernetesEnvBuildItem second = KubernetesEnvBuildItem.createFromSecret(name, TARGET, null);
        final KubernetesEnvBuildItem third = KubernetesEnvBuildItem.createFromConfigMap(name, TARGET, null);
        validator.process(first);
        validator.process(second);
        validator.process(third);
        Collection<KubernetesEnvBuildItem> items = validator.getBuildItems();
        assertEquals(3, items.size());
        assertTrue(items.contains(first) && items.contains(second) && items.contains(third));

        // check different order
        validator = new EnvVarValidator();
        validator.process(third);
        validator.process(first);
        validator.process(second);
        items = validator.getBuildItems();
        assertEquals(3, items.size());
        assertTrue(items.contains(first) && items.contains(second) && items.contains(third));
    }

    @Test
    void getBuildItemsDirectAndFromSecretShouldConflict() {
        final String name = "name";
        final String value1 = "foo";
        final String configmap = "configmap";
        final String key = "key";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createSimpleVar(name, value1, TARGET);
        final KubernetesEnvBuildItem second = KubernetesEnvBuildItem.createFromConfigMapKey(name, key, configmap, null,
                TARGET);
        validator.process(first);
        validator.process(second);
        try {
            validator.getBuildItems();
            fail();
        } catch (Exception e) {
            final String message = e.getMessage();
            assertTrue(
                    message.contains(name) && message.contains(value1) && message.contains(configmap) && message.contains(key));
        }

        // check different order
        validator = new EnvVarValidator();
        validator.process(second);
        validator.process(first);
        try {
            validator.getBuildItems();
            fail();
        } catch (Exception e) {
            final String message = e.getMessage();
            assertTrue(
                    message.contains(name) && message.contains(value1) && message.contains(configmap) && message.contains(key));
        }
    }
}
