package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

class EnvVarValidatorTest {

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
        final KubernetesEnvBuildItem initial = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, "name", "value",
                "kubernetes");
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
        final KubernetesEnvBuildItem first = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, name, value1,
                "kubernetes");
        final KubernetesEnvBuildItem second = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.field, name, value2,
                "kubernetes");
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
        final String value1 = "foo";
        final KubernetesEnvBuildItem first = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.configmap, name, value1,
                "kubernetes");
        final KubernetesEnvBuildItem second = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.configmap, name, value1,
                "kubernetes");
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
        final KubernetesEnvBuildItem first = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, name, value1,
                "kubernetes");
        final KubernetesEnvBuildItem second = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.secret, name, name,
                "kubernetes");
        final KubernetesEnvBuildItem third = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.configmap, name, name,
                "kubernetes");
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
        final KubernetesEnvBuildItem oldStyleVar = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, name, value1,
                "kubernetes", true);
        final KubernetesEnvBuildItem newStyleVar = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, name,
                "newValue",
                "kubernetes", false);
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
    void getBuildItemsTwoConflictingItemsUsingDifferentStylesShouldFail() {
        final String name = "name";
        final String value1 = "foo";
        final String value2 = "bar";
        final KubernetesEnvBuildItem first = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.var, name, value1,
                "kubernetes", true);
        final KubernetesEnvBuildItem second = new KubernetesEnvBuildItem(KubernetesEnvBuildItem.EnvType.field, name, value2,
                "kubernetes");
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
}
