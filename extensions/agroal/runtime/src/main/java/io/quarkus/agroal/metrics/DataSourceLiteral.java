package io.quarkus.agroal.metrics;

import javax.enterprise.util.AnnotationLiteral;

import io.quarkus.agroal.DataSource;

public class DataSourceLiteral extends AnnotationLiteral<DataSource> implements DataSource {

    private String name;

    public DataSourceLiteral(String name) {
        this.name = name;
    }

    @Override
    public String value() {
        return name;
    }

}
