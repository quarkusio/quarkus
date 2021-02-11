package io.quarkus.deployment.proxy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.gizmo.ClassOutput;

/**
 * Basic configuration needed to generate a proxy of a class.
 * This was inspired from jboss-invocations's org.jboss.invocation.proxy.ProxyConfiguration
 */
public class ProxyConfiguration<T> {

    private Class<?> anchorClass;
    private String proxyNameSuffix;
    private ClassLoader classLoader;
    private Class<T> superClass;
    private List<Class<?>> additionalInterfaces = new ArrayList<>(0);
    private ClassOutput classOutput;
    private boolean allowPackagePrivate = false;

    public List<Class<?>> getAdditionalInterfaces() {
        return Collections.unmodifiableList(additionalInterfaces);
    }

    public ProxyConfiguration<T> addAdditionalInterface(final Class<?> iface) {
        if (!Modifier.isInterface(iface.getModifiers())) {
            throw new IllegalArgumentException("Class " + iface.getName() + " is not an interface");
        }
        additionalInterfaces.add(iface);
        return this;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ProxyConfiguration<T> setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public Class<?> getAnchorClass() {
        return anchorClass;
    }

    public ProxyConfiguration<T> setAnchorClass(Class<?> anchorClass) {
        this.anchorClass = anchorClass;
        return this;
    }

    public String getProxyNameSuffix() {
        return proxyNameSuffix;
    }

    public ProxyConfiguration<T> setProxyNameSuffix(final String proxyNameSuffix) {
        this.proxyNameSuffix = proxyNameSuffix;
        return this;
    }

    public String getProxyName() {
        return getAnchorClass().getName() + proxyNameSuffix;
    }

    public Class<T> getSuperClass() {
        return superClass;
    }

    public ProxyConfiguration<T> setSuperClass(final Class<T> superClass) {
        this.superClass = superClass;
        return this;
    }

    public ClassOutput getClassOutput() {
        return classOutput;
    }

    public ProxyConfiguration<T> setClassOutput(ClassOutput classOutput) {
        this.classOutput = classOutput;
        return this;
    }

    public boolean isAllowPackagePrivate() {
        return allowPackagePrivate;
    }

    public ProxyConfiguration<T> setAllowPackagePrivate(boolean allowPackagePrivate) {
        this.allowPackagePrivate = allowPackagePrivate;
        return this;
    }
}
