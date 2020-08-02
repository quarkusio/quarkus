package io.quarkus.hibernate.orm.deployment;

import java.lang.reflect.Modifier;

import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

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

    public DynamicType.Unloaded<?> buildUnloadedProxy(Class<?> mappedClass, Class[] interfaces) {
        return getByteBuddyProxyHelper().buildUnloadedProxy(mappedClass, interfaces);
    }

    private ByteBuddyProxyHelper getByteBuddyProxyHelper() {
        //Lazy initialization of Byte Buddy: we'll likely need it, but if we can avoid loading it
        //in some corner cases it's worth avoiding it.
        if (this.byteBuddyProxyHelper == null) {
            bytecodeProvider = new BytecodeProviderImpl();
            this.byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
        }
        return this.byteBuddyProxyHelper;
    }

    public Class<?> uninitializedClass(String entity) {
        try {
            return Class.forName(entity, false, contextClassLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isProxiable(Class<?> mappedClass) {
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
