package io.quarkus.hibernate.orm.runtime.service.bytecodeprovider;

import static io.quarkus.hibernate.orm.runtime.service.bytecodeprovider.QuarkusRuntimeBytecodeProviderInitiator.INSTANTIATOR_SUFFIX;

import java.util.Map;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

import io.quarkus.hibernate.orm.runtime.customized.QuarkusRuntimeProxyFactoryFactory;

final class RuntimeBytecodeProvider implements BytecodeProvider {

    private final QuarkusRuntimeProxyFactoryFactory statefulProxyFactory;

    public RuntimeBytecodeProvider(QuarkusRuntimeProxyFactoryFactory statefulProxyFactory) {
        this.statefulProxyFactory = statefulProxyFactory;
    }

    @Override
    public ProxyFactoryFactory getProxyFactoryFactory() {
        return statefulProxyFactory;
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(
            Class clazz,
            String[] getterNames,
            String[] setterNames,
            Class[] types) {
        return null;
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
        try {
            Class<?> instantiatorClass = Class.forName(clazz.getName() + INSTANTIATOR_SUFFIX, true,
                    Thread.currentThread().getContextClassLoader());
            ReflectionOptimizer.InstantiationOptimizer optimizer = (ReflectionOptimizer.InstantiationOptimizer) instantiatorClass
                    .getDeclaredConstructor().newInstance();
            return new ReflectionOptimizer() {
                @Override
                public InstantiationOptimizer getInstantiationOptimizer() {
                    return optimizer;
                }

                @Override
                public AccessOptimizer getAccessOptimizer() {
                    return null;
                }
            };
        } catch (Exception e) {
            // Some mapped classes will not have an optimizer, e.g. abstract entities or classes missing a no-args constructor.
            // If we didn't find the optimizer class, just ignore the exception and return null
        }
        return null;
    }

    @Override
    public Enhancer getEnhancer(EnhancementContext enhancementContext) {
        return null;
    }
}
