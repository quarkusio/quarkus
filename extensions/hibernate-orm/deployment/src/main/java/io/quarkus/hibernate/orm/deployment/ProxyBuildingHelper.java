package io.quarkus.hibernate.orm.deployment;

import java.lang.reflect.Modifier;
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
            bytecodeProvider = new BytecodeProviderImpl(ClassFileVersion.JAVA_V21);
            this.byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
        }
        return this.byteBuddyProxyHelper;
    }

    public boolean isProxiable(ClassInfo classInfo) {
        if (classInfo == null || classInfo.isInterface() || !classInfo.hasNoArgsConstructor()) {
            return false;
        }
        // Non-final classes can always be proxied by ByteBuddy.
        // Final classes cannot. We need to check on both the classInfo and ByteBuddy as ORM 8
        // removes final from entity classes and mapped superclasses (see HHH-20512)
        if (!classInfo.isFinal()) {
            return true;
        }
        // Non-entity types (embeddables, id classes) remain final after enhancement
        // and correctly get skipped here.
        return isNonFinalAfterEnhancement(classInfo);
    }

    private boolean isNonFinalAfterEnhancement(ClassInfo classInfo) {
        TypePool.Resolution resolution = typePool.describe(classInfo.name().toString());
        return resolution.isResolved() && !Modifier.isFinal(resolution.resolve().getModifiers());
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
