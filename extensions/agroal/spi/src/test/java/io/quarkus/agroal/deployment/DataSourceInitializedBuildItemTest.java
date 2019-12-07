package io.quarkus.agroal.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataSourceInitializedBuildItemTest {

    private static final String DEFAULT_DATASOURCE = "<default>";
    private static final String NO_DEFAULT_DATASOURCE = null;

    private DataSourceInitializedBuildItem buildItem;

    @Test
    @DisplayName("datasource names are contained correctly")
    void testDataSourceNamesContained() {
        Collection<String> expectedNames = new ArrayList<>(Arrays.asList("one", "another"));
        buildItem = new DataSourceInitializedBuildItem(expectedNames, DEFAULT_DATASOURCE);
        assertEquals(expectedNames, buildItem.getDataSourceNames());
    }

    @Test
    @DisplayName("datasource names may not be changed from the outside using the given Collection")
    void testDataSourceNamesNotChangeableByGivenCollection() {
        Collection<String> expectedNames = new ArrayList<>(Arrays.asList("", "one", "another"));
        Collection<String> givenCollection = new ArrayList<>(expectedNames);
        buildItem = new DataSourceInitializedBuildItem(givenCollection, DEFAULT_DATASOURCE);
        givenCollection.add("shouldBeIgnored");
        assertEquals(expectedNames, buildItem.getDataSourceNames());
    }

    @Test
    @DisplayName("datasource names may not be changed from the outside using the returned Collection")
    void testDataSourceNamesNotChangeableByReturnedCollection() {
        Collection<String> expectedNames = new ArrayList<>(Arrays.asList("one", "another"));
        buildItem = new DataSourceInitializedBuildItem(expectedNames, DEFAULT_DATASOURCE);
        assertNotSame(expectedNames, buildItem.getDataSourceNames());
        buildItem.getDataSourceNames().add("shouldBeIgnored");
        assertEquals(expectedNames, buildItem.getDataSourceNames());
    }

    @Test
    @DisplayName("dataSourceNamesOf returns names correctly")
    void testDataSourceNamesOfInstanceReturnsNames() {
        Collection<String> expectedNames = new ArrayList<>(Arrays.asList("one", "another"));
        buildItem = new DataSourceInitializedBuildItem(expectedNames, DEFAULT_DATASOURCE);
        assertEquals(expectedNames, DataSourceInitializedBuildItem.dataSourceNamesOf(buildItem));
    }

    @Test
    @DisplayName("dataSourceNamesOf is null-safe and returns an empty set instead of null")
    void testDataSourceNamesOfInstanceIsNullSafe() {
        assertTrue(DataSourceInitializedBuildItem.dataSourceNamesOf(null).isEmpty());
    }

    @Test
    @DisplayName("isDefaultDataSourcePresent matches if there is a default datasource")
    void testIsDefaultDataSourcePresentMatches() {
        Set<String> expectedNames = new HashSet<>(Arrays.asList("", "one", "another"));
        buildItem = DataSourceInitializedBuildItem.ofDefaultDataSourceAnd(expectedNames);
        assertTrue(DataSourceInitializedBuildItem.isDefaultDataSourcePresent(buildItem));
    }

    @Test
    @DisplayName("isDefaultDataSourcePresent is null-safe and returns false in case of null")
    void testIsDefaultDataSourcePresentIsNullSafe() {
        assertFalse(DataSourceInitializedBuildItem.isDefaultDataSourcePresent(null));
    }

    @Test
    @DisplayName("default datasource is present")
    void testOnlyDefaultDataSourcePresent() {
        buildItem = new DataSourceInitializedBuildItem(Arrays.asList("any"), DEFAULT_DATASOURCE);
        assertTrue(buildItem.isDefaultDataSourcePresent());
    }

    @Test
    @DisplayName("default datasource is not present")
    void testDefaultDataSourceIsNotPresent() {
        buildItem = new DataSourceInitializedBuildItem(Arrays.asList("any"), NO_DEFAULT_DATASOURCE);
        assertFalse(buildItem.isDefaultDataSourcePresent());
    }

    @Test
    @DisplayName("createable with default datasource")
    void testSubsequentlyAddedPresentOptionalDefaultDataSourceIsPresent() {
        buildItem = DataSourceInitializedBuildItem.ofDefaultDataSourceAnd(Arrays.asList("notdefault"));
        assertTrue(buildItem.isDefaultDataSourcePresent());
    }

    @Test
    @DisplayName("createable without default datasource")
    void testSubsequentlyAddedNonPresentOptionalDefaultDataSourceDoesNotChangeAnything() {
        buildItem = DataSourceInitializedBuildItem.ofDataSources(Arrays.asList("notdefault"));
        assertSame(buildItem, buildItem);
    }

}
