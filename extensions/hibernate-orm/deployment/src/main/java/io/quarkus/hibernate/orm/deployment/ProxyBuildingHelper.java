package io.quarkus.hibernate.orm.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

/**
 * Makes it slightly more readable to interact with the Hibernate ByteBuddyProxyHelper, while improving resource
 * handling.
 */
final class ProxyBuildingHelper implements AutoCloseable {

    private static final ElementMatcher<? super MethodDescription.InDefinedShape> NO_ARG_CONSTRUCTOR = ElementMatchers
            .isConstructor().and(ElementMatchers.takesNoArguments());

    private final TypePool typePool;
    private ByteBuddyProxyHelper byteBuddyProxyHelper;
    private BytecodeProviderImpl bytecodeProvider;

    public ProxyBuildingHelper(TypePool typePool) {
        this.typePool = typePool;
    }

    public DynamicType.Unloaded<?> buildUnloadedProxy(String mappedClassName, Set<String> interfaceNames) {
        List<TypeDefinition> interfaces = new ArrayList<>();
        int i = 0;
        for (String name : interfaceNames) {
            interfaces.add(typePool.describe(name).resolve());
        }
        return getByteBuddyProxyHelper().buildUnloadedProxy(typePool, typePool.describe(mappedClassName).resolve(),
                interfaces);
    }

    private ByteBuddyProxyHelper getByteBuddyProxyHelper() {
        // Lazy initialization of Byte Buddy: we'll likely need it, but if we can avoid loading it
        // in some corner cases it's worth avoiding it.
        if (this.byteBuddyProxyHelper == null) {
            bytecodeProvider = new BytecodeProviderImpl(ClassFileVersion.JAVA_V11);
            this.byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
        }
        return this.byteBuddyProxyHelper;
    }

    public boolean isProxiable(String managedClassOrPackageName) {
        TypePool.Resolution mappedClassResolution = typePool.describe(managedClassOrPackageName);
        if (!mappedClassResolution.isResolved()) {
            // Probably a package name - consider it's not proxiable.
            return false;
        }

        TypeDescription mappedClass = mappedClassResolution.resolve();

        return !mappedClass.isFinal() && !mappedClass.getDeclaredMethods().filter(NO_ARG_CONSTRUCTOR).isEmpty();
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
