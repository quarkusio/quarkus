package io.quarkus.agroal.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item to represent a JDBC property to be set as an additional JDBC property when the datasource is setup.
 */
public final class JdbcPropertyBuildItem extends MultiBuildItem {

    private String dataSourceName;
    private String propertyName;
    private String propertyValue;

    public JdbcPropertyBuildItem(String dataSourceName, String propertyName, String propertyValue) {
        this.dataSourceName = dataSourceName;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String dataSourceName() {
        return dataSourceName;
    }

    public String propertyName() {
        return propertyName;
    }

    public String propertyValue() {
        return propertyValue;
    }
}