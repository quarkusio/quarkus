package io.quarkus.tck.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.tck.ConfigProviderTest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.quarkus.runtime.configuration.ExpandingConfigSource;

public class CustomConfigProviderTest extends ConfigProviderTest {

    @Inject
    private Config config;

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

    @Test
    public void testInjectedConfigSerializable() {
        // Needed a custom ObjectInputStream.resolveClass() to use a ClassLoader with Quarkus generated classes in it
        // Everything else is identical to the test class it overwrites
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream)) {
            out.writeObject(config);
        } catch (IOException ex) {
            Assert.fail("Injected config should be serializable, but could not serialize it", ex);
        }
        Object readObject = null;
        try (ObjectInputStream in = new CustomObjectInputStream(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            readObject = in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Assert.fail("Injected config should be serializable, but could not deserialize a previously serialized instance",
                    ex);
        }
        MatcherAssert.assertThat("Deserialized object", readObject, CoreMatchers.instanceOf(Config.class));
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
