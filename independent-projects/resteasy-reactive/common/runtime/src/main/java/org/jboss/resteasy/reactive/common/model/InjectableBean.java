package org.jboss.resteasy.reactive.common.model;

import java.util.Set;

/**
 * Class that represents information about injectable beans as we scan them, such as resource endpoint beans, or
 * BeanParam classes.
 */
public interface InjectableBean {
    /**
     * @return true if we have a FORM injectable field, either directly or in supertypes
     */
    boolean isFormParamRequired();

    InjectableBean setFormParamRequired(boolean isFormParamRequired);

    /**
     * @return true if we have injectable fields, either directly or in supertypes
     */
    boolean isInjectionRequired();

    InjectableBean setInjectionRequired(boolean isInjectionRequired);

    /**
     * @return the number of field extractors.
     */
    int getFieldExtractorsCount();

    void setFieldExtractorsCount(int fieldExtractorsCount);

    Set<String> getFileFormNames();

    void setFileFormNames(Set<String> fileFormNames);
}
