package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class RuntimeBytecodeProviderInitiator implements StandardServiceInitiator<BytecodeProvider> {

    /**
     * Singleton access
     */
    public static final RuntimeBytecodeProviderInitiator INSTANCE = new RuntimeBytecodeProviderInitiator();

    @Override
    public BytecodeProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        //This one disables any use of bytecode enhancement at runtime, but is slightly more lenient
        //than the "none" option which will throw an exception on any attempt of using it.
        return new RuntimeBytecodeProvider();
    }

    @Override
    public Class<BytecodeProvider> getServiceInitiated() {
        return BytecodeProvider.class;
    }

}
