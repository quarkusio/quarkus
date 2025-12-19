package io.quarkus.test.junit;

import static io.quarkus.registry.ValueRegistry.RuntimeKey.key;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.registry.ValueRegistry;

/**
 * A parameter resolver for JUnit to allow the resolution of {@link ValueRegistry} and {@link ValueRegistry.RuntimeInfo}
 * objects, available in {@link io.quarkus.test.junit.QuarkusTestExtension} and
 * {@link io.quarkus.test.junit.QuarkusIntegrationTestExtension}.
 */
public class ValueRegistryParameterResolver implements ParameterResolver {
    static final ValueRegistryParameterResolver INSTANCE = new ValueRegistryParameterResolver();

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        ValueRegistry valueRegistry = getValueRegistry(parameterContext, extensionContext);
        if (parameterContext.getParameter().getType().equals(ValueRegistry.class)) {
            return true;
        }
        return valueRegistry.containsKey(key(parameterContext.getParameter().getType()));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        ValueRegistry valueRegistry = getValueRegistry(parameterContext, extensionContext);
        if (parameterContext.getParameter().getType().equals(ValueRegistry.class)) {
            return valueRegistry;
        }
        return valueRegistry.get(key(parameterContext.getParameter().getType()));
    }

    private static ValueRegistry getValueRegistry(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Store store = extensionContext.getStore(Namespace.GLOBAL);
        QuarkusTestExtensionState state = store.get(QuarkusTestExtensionState.class.getName(), QuarkusTestExtensionState.class);
        if (state == null || state.getValueRegistry() == null) {
            throw new ParameterResolutionException("Could not retrieve parameter: " + parameterContext.getParameter());
        }
        return state.getValueRegistry();
    }
}
