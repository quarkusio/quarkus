package io.quarkus.hibernate.orm.deployment;

import java.util.List;

import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.jboss.jandex.ClassInfo;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;

/**
 * Makes it slightly more readable to interact with the Hibernate
 * ByteBuddyProxyHelper, while improving resource handling.
 */
final class ProxyBuildingHelper implements AutoCloseable {

    private final TypePool typePool;
    private final List<TypeDefinition> interfaces;
    private ByteBuddyProxyHelper byteBuddyProxyHelper;
    private BytecodeProviderImpl bytecodeProvider;

    public ProxyBuildingHelper(TypePool typePool) {
        this.typePool = typePool;
        this.interfaces = List.of(typePool.describe(ClassNames.HIBERNATE_PROXY.toString()).resolve());
    }

    public DynamicType.Unloaded<?> buildUnloadedProxy(String mappedClassName) {
        return getByteBuddyProxyHelper().buildUnloadedProxy(typePool, typePool.describe(mappedClassName).resolve(), interfaces);
    }

    private ByteBuddyProxyHelper getByteBuddyProxyHelper() {
        //Lazy initialization of Byte Buddy: we'll likely need it, but if we can avoid loading it
        //in some corner cases it's worth avoiding it.
        if (this.byteBuddyProxyHelper == null) {
            bytecodeProvider = new BytecodeProviderImpl(ClassFileVersion.JAVA_V11);
            this.byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
        }
        return this.byteBuddyProxyHelper;
    }

    public boolean isProxiable(ClassInfo classInfo) {
        return classInfo != null
                && !classInfo.isInterface()
                && !classInfo.isFinal()
                && classInfo.hasNoArgsConstructor();
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
