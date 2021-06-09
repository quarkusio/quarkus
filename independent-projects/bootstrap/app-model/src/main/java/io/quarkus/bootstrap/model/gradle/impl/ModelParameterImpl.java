package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.ModelParameter;
import java.io.Serializable;

public class ModelParameterImpl implements ModelParameter, Serializable {

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
