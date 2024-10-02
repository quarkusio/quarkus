package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Optional;

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
    void getBuildItemsSameItemsOldAndNewShouldWork() {
        final String name = "name";
        final String value1 = "foo";
        final KubernetesEnvBuildItem oldStyleVar = KubernetesEnvBuildItem.createSimpleVar(name, value1, TARGET, true);
        final KubernetesEnvBuildItem newStyleVar = KubernetesEnvBuildItem.createSimpleVar(name, "newValue", TARGET);
        validator.process(oldStyleVar);
        validator.process(newStyleVar);
        Collection<KubernetesEnvBuildItem> items = validator.getBuildItems();
        assertEquals(1, items.size());
        assertTrue(items.contains(newStyleVar));

        // check reverse order
        validator = new EnvVarValidator();
        validator.process(newStyleVar);
        validator.process(oldStyleVar);
        items = validator.getBuildItems();
        assertEquals(1, items.size());
        assertTrue(items.contains(newStyleVar));
    }

    @Test
    void getBuildItemsOldConflictShouldNotPreventNewToWork() {
        /*
         * quarkus.kubernetes.env.configmaps=configMap
         * quarkus.kubernetes.env-vars.xxx.configmap=configMap
         * quarkus.kubernetes.env.secrets=secret
         * quarkus.kubernetes.env-vars.xxx.secret=secret
         */
        final KubernetesEnvBuildItem newCM = KubernetesEnvBuildItem.createFromConfigMap("configmap", TARGET, null);
        final KubernetesEnvBuildItem newS = KubernetesEnvBuildItem.createFromSecret("secret", TARGET, null);
        validator.process("foo", Optional.empty(), Optional.empty(), Optional.of("configmap"), Optional.empty(),
                TARGET, Optional.empty(), true);
        validator.process(newS);
        validator.process(newCM);
        validator.process("foo", Optional.empty(), Optional.of("secret"), Optional.empty(), Optional.empty(),
                TARGET, Optional.empty(), true);
        Collection<KubernetesEnvBuildItem> items = validator.getBuildItems();
        assertEquals(2, items.size());
        assertTrue(items.contains(newCM));
        assertTrue(items.contains(newS));
    }

    @Test
    void getBuildItemsTwoConflictingItemsUsingDifferentStylesShouldFail() {
        final String name = "name";
        final String value1 = "foo";
        final String value2 = "bar";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createSimpleVar(name, value1, TARGET, true);
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

        // check different order
        validator = new EnvVarValidator();
        validator.process(second);
        validator.process(first);
        try {
            validator.getBuildItems();
            fail();
        } catch (Exception e) {
            final String message = e.getMessage();
            assertTrue(message.contains(name) && message.contains(value1) && message.contains(value2));
        }
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

    @Test
    void getBuildItemsUsingOldStyleProcessAndNewStyleCreateForSameItemShouldKeepNewStyle() {
        final String name = "name";
        final String configmap = "configmap";
        final String key = "key";
        final KubernetesEnvBuildItem first = KubernetesEnvBuildItem.createFromConfigMapKey(name, key, configmap, null, TARGET);
        validator.process(first);
        validator.process(name, Optional.of("oldKey"), Optional.empty(), Optional.of(configmap), Optional.empty(),
                TARGET, Optional.empty(), true);
        Collection<KubernetesEnvBuildItem> buildItems = validator.getBuildItems();
        assertEquals(1, buildItems.size());
        assertTrue(buildItems.contains(first));

        // check different order
        validator = new EnvVarValidator();
        validator.process(name, Optional.of("oldKey"), Optional.empty(), Optional.of(configmap), Optional.empty(),
                TARGET, Optional.empty(), true);
        validator.process(first);
        buildItems = validator.getBuildItems();
        assertEquals(1, buildItems.size());
        assertTrue(buildItems.contains(first));
    }
}
