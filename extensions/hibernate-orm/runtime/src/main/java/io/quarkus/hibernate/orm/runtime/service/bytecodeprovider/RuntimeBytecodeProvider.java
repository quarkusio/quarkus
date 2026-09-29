package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;

import io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactory;

final class RuntimeBytecodeProvider implements BytecodeProvider {

    private final QuarkusRuntimeProxyFactoryFactory preGeneratedProxyFactory;

    public RuntimeBytecodeProvider(QuarkusRuntimeProxyFactoryFactory preGeneratedProxyFactory) {
        this.preGeneratedProxyFactory = preGeneratedProxyFactory;
    }

    @Override
    public ProxyFactoryFactory getProxyFactoryFactory() {
        return preGeneratedProxyFactory;
    }

    @Override
    public EnhancementSession createEnhancementSession(EnhancementModel enhancementModel,
            EnhancementEnvironment enhancementEnvironment) {
        return null;
    }
}
