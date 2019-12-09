package io.quarkus.agroal.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating the datasource has been fully initialized.
 * <p>
 * Contains all processed datasource names, including an empty string for the default datasource.
 */
public final class DataSourceInitializedBuildItem extends SimpleBuildItem {
    private static final String DEFAULT_DATASOURCE_NAME = "<default>";

    private final String defaultDataSourceName;
    private final Collection<String> dataSourceNames = new ArrayList<>();

    /**
     * Null-safe way to get the datasource names of the given {@link DataSourceInitializedBuildItem}.
     */
    public static final Collection<String> dataSourceNamesOf(DataSourceInitializedBuildItem buildItem) {
        return (buildItem != null) ? buildItem.getDataSourceNames() : Collections.emptyList();
    }

    /**
     * Null-safe way to find out, if the given {@link DataSourceInitializedBuildItem} contains the default datasource.
     */
    public static final boolean isDefaultDataSourcePresent(DataSourceInitializedBuildItem buildItem) {
        return (buildItem != null) ? buildItem.isDefaultDataSourcePresent() : false;
    }

    /**
     * Creates a new instance of the default DataSource name and the given DataSource names.
     */
    public static final DataSourceInitializedBuildItem ofDefaultDataSourceAnd(Collection<String> dataSourceNames) {
        return new DataSourceInitializedBuildItem(dataSourceNames, DEFAULT_DATASOURCE_NAME);
    }

    /**
     * Creates a new instance of the given DataSource names.
     */
    public static final DataSourceInitializedBuildItem ofDataSources(Collection<String> dataSourceNames) {
        Collection<String> allDataSourceNames = new ArrayList<>(dataSourceNames);
        return new DataSourceInitializedBuildItem(allDataSourceNames, null);
    }

    DataSourceInitializedBuildItem(Collection<String> dataSourceNames, String defaultDataSourceName) {
        this.dataSourceNames.addAll(dataSourceNames);
        this.defaultDataSourceName = defaultDataSourceName;
    }

    public Collection<String> getDataSourceNames() {
        return new ArrayList<>(dataSourceNames);
    }

    public boolean isDefaultDataSourcePresent() {
        return (defaultDataSourceName != null);
    }

    @Override
    public String toString() {
        return "DataSourceInitializedBuildItem [defaultDataSourceName=" + defaultDataSourceName + ", dataSourceNames="
                + dataSourceNames + "]";
    }
}
