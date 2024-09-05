package io.quarkus.test.common;

import static io.quarkus.test.common.TestResourceManager.testResourcesRequireReload;
import static io.quarkus.test.common.TestResourceScope.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
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
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS))));

        assertTrue(testResourcesRequireReload(Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS)),
                Collections.emptySet()));
    }

    @Test
    public void sameSingleRestrictedToClassResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS)),
                Set.of(new TestResourceComparisonInfo("test", RESTRICTED_TO_CLASS))));
    }

    @Test
    public void sameSingleMatchingResource() {
        assertFalse(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES)),
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES))));
    }

    @Test
    public void differentSingleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(new TestResourceComparisonInfo("test", MATCHING_RESOURCES)),
                Set.of(new TestResourceComparisonInfo("test2", MATCHING_RESOURCES))));
    }

    @Test
    public void sameMultipleMatchingResource() {
        assertFalse(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test3", GLOBAL)),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES))));
    }

    @Test
    public void differentMultipleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test3", GLOBAL)),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES))));
    }

    @Test
    public void differentGlobalMultipleMatchingResource() {
        assertTrue(testResourcesRequireReload(
                Set.of(
                        new TestResourceComparisonInfo("test", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("test4", GLOBAL)),
                Set.of(new TestResourceComparisonInfo("test3", GLOBAL),
                        new TestResourceComparisonInfo("test2", MATCHING_RESOURCES),
                        new TestResourceComparisonInfo("TEST", MATCHING_RESOURCES))));
    }
}
