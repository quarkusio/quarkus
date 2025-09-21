package io.quarkus.micrometer.runtime.binder;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.micrometer.runtime.config.MicrometerConfig;

public class MicrometerConfigurationTest {

    @Test
    void micrometerEnabledFalseAndEnabledAllTrue() {
        MicrometerConfig mockConfig = Mockito.mock(MicrometerConfig.class);

        Mockito.when(mockConfig.enabled()).thenReturn(false);

        Assertions.assertFalse(mockConfig.isEnabled(new MicrometerConfig.CapabilityEnabled() {
            @Override
            public Optional<Boolean> enabled() {
                return Optional.of(true);
            }
        }));
    }

    @Test
    void enableAllTrueAndBinderJvmTrue() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.jvm()).thenReturn(Optional.of(false));
        Mockito.when(binderConfig.enableAll()).thenReturn(true);

        boolean isEnabled = micrometerConfig.isEnabled(micrometerConfig.binder().jvm());

        Assertions.assertTrue(isEnabled);
    }

    @Test
    void binderJvmTrueAndEnableAllFalse() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.jvm()).thenReturn(Optional.of(true));
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        boolean isEnabled = micrometerConfig.isEnabled(micrometerConfig.binder().jvm());

        Assertions.assertTrue(isEnabled);
    }

    @Test
    void enabledTrue_enableAllFalse_optionalEmpty_returnsFalse() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        boolean isEnabled = micrometerConfig.isEnabled(Optional.empty());
        Assertions.assertFalse(isEnabled);
    }

    @Test
    void enabledTrue_enableAllFalse_optionalTrue_returnsTrue() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        boolean isEnabled = micrometerConfig.isEnabled(Optional.of(true));
        Assertions.assertTrue(isEnabled);
    }

    @Test
    void enabledTrue_enableAllFalse_optionalFalse_returnsFalse() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        boolean isEnabled = micrometerConfig.isEnabled(Optional.of(false));
        Assertions.assertFalse(isEnabled);
    }

    @Test
    void enabledTrue_enableAllFalse_capabilityEnabledTrue_returnsTrue() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        MicrometerConfig.CapabilityEnabled capability = () -> Optional.of(true);
        Assertions.assertTrue(micrometerConfig.isEnabled(capability));
    }

    @Test
    void enabledTrue_enableAllFalse_capabilityEnabledFalse_returnsFalse() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        MicrometerConfig.CapabilityEnabled capability = () -> Optional.of(false);
        Assertions.assertFalse(micrometerConfig.isEnabled(capability));
    }

    @Test
    void enabledTrue_enableAllFalse_capabilityEnabledEmpty_returnsFalse() {
        MicrometerConfig micrometerConfig = Mockito.mock(MicrometerConfig.class, Mockito.CALLS_REAL_METHODS);
        MicrometerConfig.BinderConfig binderConfig = Mockito.mock(MicrometerConfig.BinderConfig.class);

        Mockito.when(micrometerConfig.enabled()).thenReturn(true);
        Mockito.when(micrometerConfig.binder()).thenReturn(binderConfig);
        Mockito.when(binderConfig.enableAll()).thenReturn(false);

        MicrometerConfig.CapabilityEnabled capability = Optional::empty;
        Assertions.assertFalse(micrometerConfig.isEnabled(capability));
    }
}
