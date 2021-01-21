package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import net.bytebuddy.ClassFileVersion;

/**
 * We need a different implementation of ProxyFactoryFactory during the build than at runtime,
 * so to allow metadata validation. This implementation is then swapped after the metadata has been recorded.
 * 
 * @see io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactory
 */
public final class BootstrapOnlyProxyFactoryFactoryInitiator implements StandardServiceInitiator<ProxyFactoryFactory> {

    /**
     * Singleton access
     */
    public static final StandardServiceInitiator<ProxyFactoryFactory> INSTANCE = new BootstrapOnlyProxyFactoryFactoryInitiator();

    @Override
    public ProxyFactoryFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        BytecodeProviderImpl bbProvider = new BytecodeProviderImpl(ClassFileVersion.JAVA_V8);
        return bbProvider.getProxyFactoryFactory();
    }

    @Override
    public Class<ProxyFactoryFactory> getServiceInitiated() {
        return ProxyFactoryFactory.class;
    }
}
