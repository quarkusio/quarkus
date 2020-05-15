package io.quarkus.hibernate.orm.deployment;

import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;

final class EntitiesClassLoader extends ClassLoader {

    private final TransformedClassesBuildItem transformedEntities;
    private final ClassLoader parent;
    private final ConcurrentMap<ClassPathElement, ProtectionDomain> protectionDomains = new ConcurrentHashMap<>();

    EntitiesClassLoader(TransformedClassesBuildItem transformedEntities, ClassLoader parent) {
        this.transformedEntities = transformedEntities;
        this.parent = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final TransformedClassesBuildItem.TransformedClass transformedClass = transformedEntities
                .getTransformedClassByName(name);
        if (transformedClass != null) {
            final byte[] data = transformedClass.getData();
            return defineClass(name, data, 0, data.length, null);
            //                    protectionDomains.computeIfAbsent(classPathElement, (ce) -> ce.getProtectionDomain(this)));
        } else {
            return parent.loadClass(name);
        }
    }

}
