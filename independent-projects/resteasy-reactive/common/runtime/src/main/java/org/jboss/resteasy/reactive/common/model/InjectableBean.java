package org.jboss.resteasy.reactive.common.model;

/**
 * Class that represents information about injectable beans as we scan them, such as
 * resource endpoint beans, or BeanParam classes.
 */
public interface InjectableBean {
    /**
     * @return true if we have a FORM injectable field, either directly or in supertypes
     */
    public boolean isFormParamRequired();

    public InjectableBean setFormParamRequired(boolean isFormParamRequired);

    /**
     * @return true if we have injectable fields, either directly or in supertypes
     */
    public boolean isInjectionRequired();

    public InjectableBean setInjectionRequired(boolean isInjectionRequired);
}
