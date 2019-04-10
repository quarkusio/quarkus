package io.quarkus.deployment.recording;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtilsBean;

/**
 * <p>
 * Delegates to PropertyUtilsBean from Apache Commons Beanutils;
 * must be closed to release any internal caches.
 * </p>
 *
 * <p>
 * This is inspired by org.apache.commons.beanutils.PropertyUtils, but
 * only exposing the selected methods we use and to avoid allocating some objects
 * which aren't necessary in our limited use cases; also bypasses the classloader
 * scoped cache in favour of an explicit cache control on close, to keep things
 * a bit lighter.
 * </p>
 *
 * @see org.apache.commons.beanutils.PropertyUtils
 * @see org.apache.commons.beanutils.PropertyUtilsBean
 */
final class PropertyUtils implements AutoCloseable {

    /**
     * Implementation note: the reference to PropertyUtilsBean is static so to avoid allocating multiple of those,
     * yet we close its cache when this instance is closed.
     * We're effectively assuming non current, singleton usage.
     * Although if this is not respected, worse that could happen is some extra cache misses.
     */
    private static final PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();

    /**
     * <p>
     * Return the value of the specified property of the specified bean,
     * no matter which property reference format is used, with no
     * type conversions.
     * </p>
     *
     * <p>
     * For more details see <code>PropertyUtilsBean</code>.
     * </p>
     *
     * @param bean Bean whose property is to be extracted
     * @param name Possibly indexed and/or nested name of the property
     *        to be extracted
     * @return the property value
     *
     * @throws IllegalAccessException if the caller does not have
     *         access to the property accessor method
     * @throws IllegalArgumentException if <code>bean</code> or
     *         <code>name</code> is null
     * @throws InvocationTargetException if the property accessor method
     *         throws an exception
     * @throws NoSuchMethodException if an accessor method for this
     *         property cannot be found
     */
    public Object getProperty(final Object bean, final String name)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return propertyUtilsBean.getNestedProperty(bean, name);
    }

    /**
     * <p>
     * Retrieve the property descriptors for the specified bean,
     * introspecting and caching them the first time a particular bean class
     * is encountered.
     * </p>
     *
     * <p>
     * For more details see <code>PropertyUtilsBean</code>.
     * </p>
     *
     * @param bean Bean for which property descriptors are requested
     * @return the property descriptors
     * @throws IllegalArgumentException if <code>bean</code> is null
     */
    public PropertyDescriptor[] getPropertyDescriptors(final Object bean) {
        return propertyUtilsBean.getPropertyDescriptors(bean);
    }

    /**
     * Clears all caches of PropertyUtilsBean and Introspector
     *
     * @see Introspector
     * @see PropertyUtilsBean
     */
    public void close() {
        propertyUtilsBean.clearDescriptors();
    }

}
