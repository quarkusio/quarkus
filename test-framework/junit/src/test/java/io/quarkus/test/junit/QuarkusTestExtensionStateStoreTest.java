package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;

class QuarkusTestExtensionStateStoreTest {

    @Test
    void reusesStateAndRuntimeContextAcrossMavenExecutionRequests() {
        String surefireClassPath = System.getProperty("surefire.real.class.path");
        System.setProperty("surefire.real.class.path", "test");

        ExtensionContext firstContext = context();
        ExtensionContext secondContext = context();
        QuarkusTestExtensionState state = mock(QuarkusTestExtensionState.class);
        QuarkusTestExtension extension = new QuarkusTestExtension();
        ValueRegistry valueRegistry = mock(ValueRegistry.class);
        Config config = mock(Config.class);
        when(firstContext.getStore(GLOBAL).get(ValueRegistry.class.getName())).thenReturn(valueRegistry);
        when(firstContext.getStore(GLOBAL).get(Config.class.getName())).thenReturn(config);

        try {
            extension.setState(firstContext, state);

            assertThat(extension.getState(secondContext)).isSameAs(state);
            assertThat(extension.getState(secondContext)).isSameAs(state);
            verify(secondContext.getStore(GLOBAL)).put(ValueRegistry.class.getName(), valueRegistry);
            verify(secondContext.getStore(GLOBAL)).put(Config.class.getName(), config);
        } finally {
            extension.setState(firstContext, null);
            restoreSurefireClassPath(surefireClassPath);
        }
    }

    private static ExtensionContext context() {
        ExtensionContext context = mock(ExtensionContext.class);
        ExtensionContext root = mock(ExtensionContext.class);
        ExtensionContext.Store rootStore = mock(ExtensionContext.Store.class);
        ExtensionContext.Store contextStore = mock(ExtensionContext.Store.class);

        when(context.getRoot()).thenReturn(root);
        when(root.getStore(GLOBAL)).thenReturn(rootStore);
        when(context.getStore(GLOBAL)).thenReturn(contextStore);

        return context;
    }

    private static void restoreSurefireClassPath(String surefireClassPath) {
        if (surefireClassPath == null) {
            System.clearProperty("surefire.real.class.path");
        } else {
            System.setProperty("surefire.real.class.path", surefireClassPath);
        }
    }
}
