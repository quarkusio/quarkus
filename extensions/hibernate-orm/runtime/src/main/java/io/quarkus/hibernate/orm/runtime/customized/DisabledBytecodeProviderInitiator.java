package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.internal.none.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class DisabledBytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {

    /**
     * Singleton access
     */
    public static final StandardServiceInitiator<BytecodeProvider> INSTANCE = new DisabledBytecodeProviderInitiator();

    @Override
    public BytecodeProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //This one disables any use of bytecode enhancement at runtime.
        return new BytecodeProviderImpl();
    }

    @Override
    public Class<BytecodeProvider> getServiceInitiated() {
        return BytecodeProvider.class;
    }

}
