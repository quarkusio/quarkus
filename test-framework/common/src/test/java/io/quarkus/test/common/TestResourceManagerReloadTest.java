package io.quarkus.test.common;

import static io.quarkus.test.common.TestResourceManager.getReloadGroupIdentifier;
import static io.quarkus.test.common.TestResourceManager.testResourcesRequireReload;
import static io.quarkus.test.common.TestResourceScope.GLOBAL;
import static io.quarkus.test.common.TestResourceScope.MATCHING_RESOURCES;
import static io.quarkus.test.common.TestResourceScope.RESTRICTED_TO_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.TestResourceManager.TestResourceComparisonInfo;

public class TestResourceManagerReloadTest {

    @Test
    public void emptyResources() {
        Set<TestResourceComparisonInfo> existing = Collections.emptySet();
        Set<TestResourceComparisonInfo> next = Set.of();
        assertFalse(testResourcesRequireReload(existing, next));
        assertEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void differentCount() {
        Set<TestResourceComparisonInfo> existing = Collections.emptySet();
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of()));

        assertTrue(testResourcesRequireReload(existing, next));
        assertTrue(testResourcesRequireReload(next, existing));

        assertNotEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameSingleRestrictedToClassResource() {
        Set<TestResourceComparisonInfo> existing = Set
                .of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS, Map.of()));
        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameSingleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()));

        assertFalse(testResourcesRequireReload(existing, next));

        assertEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameSingleMatchingResourceWithArgs() {
        Set<TestResourceComparisonInfo> existing = Set
                .of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")));
        Set<TestResourceComparisonInfo> next = Set
                .of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")));

        assertFalse(testResourcesRequireReload(existing, next));

        assertEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameSingleResourceDifferentArgs() {
        Set<TestResourceComparisonInfo> existing = Set
                .of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()));

        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameSingleResourceDifferentArgValues() {
        Set<TestResourceComparisonInfo> existing = Set
                .of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "b")));
        Set<TestResourceComparisonInfo> next = Set
                .of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of("a", "x")));

        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void differentSingleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()));
        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));
    }

    @Test
    public void sameMultipleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test3", GLOBAL, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()));

        assertFalse(testResourcesRequireReload(existing, next));
        assertEquals(getReloadGroupIdentifier(existing), getReloadGroupIdentifier(next));

    }

    @Test
    public void differentMultipleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test3", GLOBAL, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES, Map.of()));
        assertTrue(testResourcesRequireReload(existing, next));
        assertNotEquals(getReloadGroupIdentifier(existing),
                getReloadGroupIdentifier(next));
    }

    @Test
    public void differentGlobalMultipleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("test4", GLOBAL, Map.of()));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test3", GLOBAL, Map.of()),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES, Map.of()),
                new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES, Map.of()));

        assertTrue(testResourcesRequireReload(existing, next));
        assertNotEquals(getReloadGroupIdentifier(existing),
                getReloadGroupIdentifier(next));

    }
}
