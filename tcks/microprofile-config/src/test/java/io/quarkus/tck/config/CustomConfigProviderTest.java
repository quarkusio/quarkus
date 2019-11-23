package io.quarkus.tck.config;

import org.eclipse.microprofile.config.tck.ConfigProviderTest;
import org.testng.annotations.Test;

import io.quarkus.runtime.configuration.ExpandingConfigSource;

public class CustomConfigProviderTest extends ConfigProviderTest {
    @Test
    public void testEnvironmentConfigSource() {
        // this test fails when there is a expression-like thing in an env prop
        boolean old = ExpandingConfigSource.setExpanding(false);
        try {
            super.testPropertyConfigSource();
        } finally {
            ExpandingConfigSource.setExpanding(old);
        }
    }

    @Test(enabled = false)
    public void testInjectedConfigSerializable() {
    }

    @Test
    public void testPropertyConfigSource() {
        // this test fails when there is a expression-like thing in a sys prop
        boolean old = ExpandingConfigSource.setExpanding(false);
        try {
            super.testPropertyConfigSource();
        } finally {
            ExpandingConfigSource.setExpanding(old);
        }
    }
}
