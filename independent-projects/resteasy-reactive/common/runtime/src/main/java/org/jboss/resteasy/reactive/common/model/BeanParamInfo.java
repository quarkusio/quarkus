package org.jboss.resteasy.reactive.common.model;

public class BeanParamInfo implements InjectableBean {
    private boolean isFormParamRequired;
    private boolean isInjectionRequired;
    private int fieldExtractorsCount;

    @Override
    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    @Override
    public InjectableBean setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }

    @Override
    public boolean isInjectionRequired() {
        return isInjectionRequired;
    }

    @Override
    public InjectableBean setInjectionRequired(boolean isInjectionRequired) {
        this.isInjectionRequired = isInjectionRequired;
        return this;
    }

    @Override
    public int getFieldExtractorsCount() {
        return fieldExtractorsCount;
    }

    @Override
    public void setFieldExtractorsCount(int fieldExtractorsCount) {
        this.fieldExtractorsCount = fieldExtractorsCount;
    }
}
