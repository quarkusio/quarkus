package io.quarkus.test.common;

import static io.quarkus.test.common.TestResourceManager.testResourcesReloadKey;
import static io.quarkus.test.common.TestResourceManager.testResourcesRequireReload;
import static io.quarkus.test.common.TestResourceScope.GLOBAL;
import static io.quarkus.test.common.TestResourceScope.MATCHING_RESOURCES;
import static io.quarkus.test.common.TestResourceScope.RESTRICTED_TO_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.TestResourceManager.TestResourceComparisonInfo;

public class TestResourceManagerReloadTest {

    @Test
    public void emptyResources() {
        Set<TestResourceComparisonInfo> existing = Collections.emptySet();
        Set<TestResourceComparisonInfo> next = Set.of();
        assertFalse(testResourcesRequireReload(existing, next));
        assertEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));
    }

    @Test
    public void differentCount() {
        Set<TestResourceComparisonInfo> existing = Collections.emptySet();
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS));

        assertTrue(testResourcesRequireReload(existing, next));
        assertTrue(testResourcesRequireReload(next, existing));

        assertNotEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));
    }

    @Test
    public void sameSingleRestrictedToClassResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS));
        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));
    }

    @Test
    public void sameSingleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES));

        assertFalse(testResourcesRequireReload(existing, next));

        assertEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));
    }

    @Test
    public void differentSingleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test2", MATCHING_RESOURCES));
        assertTrue(testResourcesRequireReload(existing, next));

        assertNotEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));
    }

    @Test
    public void sameMultipleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("test3", GLOBAL));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test3", GLOBAL),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES));

        assertFalse(testResourcesRequireReload(existing, next));
        assertEquals(testResourcesReloadKey(existing), testResourcesReloadKey(next));

    }

    @Test
    public void differentMultipleMatchingResource() {
        Set<TestResourceComparisonInfo> existing = Set.of(
                new TestResourceComparisonInfo("test", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("test3", GLOBAL));
        Set<TestResourceComparisonInfo> next = Set.of(new TestResourceComparisonInfo("test3", GLOBAL),
                new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES));
        assertTrue(testResourcesRequireReload(existing, next));
        assertNotEquals(testResourcesReloadKey(existing),
                testResourcesReloadKey(next));
    }
}
