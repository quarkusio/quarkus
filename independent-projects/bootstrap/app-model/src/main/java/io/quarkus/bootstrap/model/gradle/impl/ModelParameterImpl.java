package io.quarkus.bootstrap.model.gradle.impl;

import java.io.Serializable;

import io.quarkus.bootstrap.model.gradle.ModelParameter;

public class ModelParameterImpl implements ModelParameter, Serializable {

    private static final long serialVersionUID = 4617775770506785059L;

    private String mode;

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public void setMode(String mode) {
        this.mode = mode;
    }
}
