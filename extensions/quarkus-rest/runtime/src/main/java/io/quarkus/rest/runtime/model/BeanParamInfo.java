package io.quarkus.rest.runtime.model;

public class BeanParamInfo implements FormContainer {
    boolean isFormParamRequired;

    @Override
    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    @Override
    public FormContainer setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }
}
