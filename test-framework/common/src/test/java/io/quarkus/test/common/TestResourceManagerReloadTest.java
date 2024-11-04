package io.quarkus.test.common;

import static io.quarkus.test.common.TestResourceManager.testResourcesRequireReload;
import static io.quarkus.test.common.TestResourceScope.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.TestResourceManager.TestResourceComparisonInfo;

public class TestResourceManagerReloadTest {

    @Test
    public void emptyResources() {
        assertFalse(testResourcesRequireReload(Collections.emptySet(), Set.of()));
    }

    @Test
    public void differentCount() {
        assertTrue(testResourcesRequireReload(Collections.emptySet(),
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of()))));

        assertTrue(testResourcesRequireReload(Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of())),
                Collections.emptySet()));
    }

    @Test
    public void sameSingleRestrictedToClassResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of())),
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of()))));
    }

    @Test
    public void sameSingleMatchingResource() {
        assertFalse(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of())),
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()))));
    }

    @Test
    public void sameSingleMatchingResourceWithArgs() {
        assertFalse(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b"))),
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")))));
    }

    @Test
    public void sameSingleResourceDifferentArgs() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of())),
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")))));
    }

    @Test
    public void sameSingleResourceDifferentArgValues() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b"))),
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "c")))));
    }

    @Test
    public void differentSingleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of())),
                Set.of(new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()))));
    }

    @Test
    public void sameMultipleMatchingResource() {
        assertFalse(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test3", GLOBAL, Map.of())),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()))));
    }

    @Test
    public void differentMultipleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test3", GLOBAL, Map.of())),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES, Map.of()))));
    }

    @Test
    public void differentGlobalMultipleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("test4", GLOBAL, Map.of())),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                        new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES, Map.of()))));
    }
}
