package io.quarkus.deployment.proxy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic configuration needed to generate a proxy of a class.
 * This was inspired from jboss-invocations's org.jboss.invocation.proxy.ProxyConfiguration
 */
public class ProxyConfiguration<T> {

    private String proxyName = null;
    private ClassLoader classLoader;
    private Class<T> superClass;
    private List<Class<?>> additionalInterfaces = new ArrayList<>(0);

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

    public String getProxyName() {
        return proxyName;
    }

    public ProxyConfiguration<T> setProxyName(final String proxyName) {
        this.proxyName = proxyName;
        return this;
    }

    public ProxyConfiguration<T> setProxyName(final Package pkg, final String simpleName) {
        this.proxyName = pkg.getName() + '.' + simpleName;
        return this;
    }

    public Class<T> getSuperClass() {
        return superClass;
    }

    public ProxyConfiguration<T> setSuperClass(final Class<T> superClass) {
        this.superClass = superClass;
        return this;
    }
}
