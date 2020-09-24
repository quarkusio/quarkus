package io.quarkus.rest.runtime.model;

public interface FormContainer {
    public boolean isFormParamRequired();

    public FormContainer setFormParamRequired(boolean isFormParamRequired);
}
