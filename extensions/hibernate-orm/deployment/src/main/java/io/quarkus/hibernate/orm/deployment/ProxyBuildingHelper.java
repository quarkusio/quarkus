package io.quarkus.hibernate.orm.deployment;

import java.lang.reflect.Modifier;
import java.util.Set;

import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;

/**
 * Makes it slightly more readable to interact with the Hibernate
 * ByteBuddyProxyHelper, while improving resource handling.
 */
final class ProxyBuildingHelper implements AutoCloseable {

    private final ClassLoader contextClassLoader;
    private ByteBuddyProxyHelper byteBuddyProxyHelper;
    private BytecodeProviderImpl bytecodeProvider;

    public ProxyBuildingHelper(ClassLoader contextClassLoader) {
        this.contextClassLoader = contextClassLoader;
    }

    public DynamicType.Unloaded<?> buildUnloadedProxy(String mappedClassName, Set<String> interfaceNames) {
        final Class[] interfaces = new Class[interfaceNames.size()];
        int i = 0;
        for (String name : interfaceNames) {
            interfaces[i++] = uninitializedClass(name);
        }
        final Class<?> mappedClass = uninitializedClass(mappedClassName);
        return getByteBuddyProxyHelper().buildUnloadedProxy(mappedClass, interfaces);
    }

    private ByteBuddyProxyHelper getByteBuddyProxyHelper() {
        //Lazy initialization of Byte Buddy: we'll likely need it, but if we can avoid loading it
        //in some corner cases it's worth avoiding it.
        if (this.byteBuddyProxyHelper == null) {
            bytecodeProvider = new BytecodeProviderImpl(ClassFileVersion.JAVA_V8);
            this.byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
        }
        return this.byteBuddyProxyHelper;
    }

    private Class<?> uninitializedClass(String entity) {
        try {
            return Class.forName(entity, false, contextClassLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isProxiable(String managedClassOrPackageName) {
        Class<?> mappedClass;
        try {
            mappedClass = Class.forName(managedClassOrPackageName, false, contextClassLoader);
        } catch (ClassNotFoundException e) {
            // Probably a package name - consider it's not proxiable.
            return false;
        }

        if (Modifier.isFinal(mappedClass.getModifiers())) {
            return false;
        }
        try {
            mappedClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        if (bytecodeProvider != null) {
            bytecodeProvider.resetCaches();
            bytecodeProvider = null;
            byteBuddyProxyHelper = null;
        }
    }
}
