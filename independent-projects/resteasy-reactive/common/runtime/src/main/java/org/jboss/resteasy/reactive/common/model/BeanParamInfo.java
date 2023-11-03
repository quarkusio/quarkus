package org.jboss.resteasy.reactive.common.model;

import java.util.HashSet;
import java.util.Set;

public class BeanParamInfo implements InjectableBean {
    private boolean isFormParamRequired;
    private boolean isInjectionRequired;
    private int fieldExtractorsCount;
    private Set<String> fileFormNames = new HashSet<>();

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

    @Override
    public Set<String> getFileFormNames() {
        return fileFormNames;
    }

    @Override
    public void setFileFormNames(Set<String> fileFormNames) {
        this.fileFormNames = fileFormNames;
    }
}
